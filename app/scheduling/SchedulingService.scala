package scheduling

import java.sql.Date

import javax.inject.{Inject, Singleton}
import scheduling.constraints._
import scheduling.models.SchedulingModel

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchedulingService @Inject()(schedulingModel: SchedulingModel)(implicit ec: ExecutionContext) {

  def buildSchedule(project: Int): Future[SchedulingResult] = {
    // 1st regen all slots
    schedulingModel.buildSlotsForProject(project).flatMap { _ =>

      // 2nd get data
      schedulingModel.getScheduleData(project).map {
        case (proj, staffs, slots, constraints) =>
          println("Generating planning for " + slots.size + " slots and " + staffs.size + " staffs")
          computePlanification(proj, staffs, slots, constraints)
      }.flatMap { case (list, result) => {
        println("DONE computing schedule, generated " + list.size + " assignations")
        schedulingModel.pushSchedule(list).map(_ => result)
      }
      }
    }
  }


  private def computePlanification(project: ScheduleProject, staffs: Seq[scheduling.Staff], slots: Iterable[scheduling.TaskSlot], constraintsSeq: Seq[ScheduleConstraint]): (List[StaffAssignation], SchedulingResult) = {
    implicit val periodsOrdering: Ordering[Period] = (x, y) => {
      if (x.day == y.day) {
        if (x.timeStart != y.timeStart) x.timeStart - y.timeStart
        else x.timeEnd - y.timeEnd
      } else (x.day.getTime - y.day.getTime).toInt
    }

    implicit val slotsOrdering: Ordering[TaskSlot] = (x, y) => {
      val a = periodsOrdering.compare(x.timeSlot, y.timeSlot)
      if (a == 0) x.id - y.id else a
    }

    val (preConstraints, constraints) = constraintsSeq.partition(_.isInstanceOf[PreProcessConstraint]) match {
      case (pre, con) => pre.map(_.asInstanceOf[PreProcessConstraint]) -> con.map(_.asInstanceOf[ProcessConstraint])
    }

    // Create variables
    // Algorithm by Tony Clavien
    val notAttributed = mutable.Set[TaskSlot]() // Slots for which no-one was found

    val attributions = mutable.Map[TaskSlot, mutable.HashSet[Staff]]()

    /**
     * Get all the attributed tasks for a given staff
     */
    def attributionsFor(staff: Staff): Iterable[TaskSlot] = attributions.filter(_._2.contains(staff)).keys

    /**
     * Count the total time done by a staff
     */
    def countTime(staff: Staff): Int = attributionsFor(staff).toList.map(_.timeSlot.duration).sum

    /**
     * Count the total time done by a staff
     */
    def countTimeOnDay(staff: Staff, day: Date): Int = attributionsFor(staff).toList.filter(_.timeSlot.day == day).map(_.timeSlot.duration).sum

    implicit val staffsOrdering: Ordering[Staff] = (x, y) => {
      val a = countTime(x) - countTime(y)
      if (a == 0) x.user.userId - y.user.userId else a
    }

    var staffsToAttribute = staffs.sorted.toList

    /**
     * Give a task to a staff
     */
    def attribute(slot: TaskSlot, staff: Staff): Boolean = {
      if (!attributions.contains(slot)) {
        attributions.put(slot, mutable.HashSet())
      }

      attributions(slot) += staff
      staffsToAttribute = staffsToAttribute.sorted // re-sort the list
      attributions(slot).size >= slot.staffsRequired
    }


    /**
     * Check if a staff is already busy during a given slot
     */
    def isBusy(staff: Staff, slot: TaskSlot): Boolean = {
      attributionsFor(staff)
        .map(_.timeSlot)
        .exists(period => period.isOverlappingWithBreakTime(slot.timeSlot, project.minBreakMinutes))
    }

    /**
     * Check if a given staff is able (has required experience, age and abilities) to do a task
     */
    def isAble(staff: Staff, slot: TaskSlot): Boolean = {
      staff.experience >= slot.task.minExperience &&
        staff.age >= slot.task.minAge &&
        slot.task.difficulties.forall(diff => staff.capabilities.contains(diff))
    }

    def hasEnoughRemainingTime(staff: Staff, slot: TaskSlot): Boolean = {
      val time = countTimeOnDay(staff, slot.timeSlot.day) + slot.timeSlot.duration
      time <= project.maxTimePerStaff * 60
    }


    var slotsAttributed = Set[TaskSlot]()
    // Pre-attributed slots
    val staffsMap = staffs.map(s => (s.user.userId -> s)).toMap
    val slotsMap = slots.map(s => (s.id -> s)).toMap
    preConstraints.foreach {
      case FixedTaskSlotConstraint(_, _, staffId, slotId) =>
        val staff = staffsMap(staffId)
        val slot = slotsMap(slotId)

        if (attribute(slot, staff)) {
          slotsAttributed = slotsAttributed + slot
        }

      case _ =>
    }


    val toAttribute = slots.filter(slot => !slotsAttributed(slot)).toList.sorted

    /**
     * Logique d'attribution :
     * Tant que il y a des slots à attribuer
     * et tant qu'on a pas trouvé de staff qui lui conviennent
     * on récupère le staff suivant selon leur nombre d'heure
     * on vérifie que les contraintes sont respectées pour ces 2 élément
     * si ça marche on attribueslot ::
     * // sinon on passe au staff suivant
     * Si on arrive au bout et qu'on a pas trouvé de staff qui satisfassent les contraintes
     * on l'ajoute au slot non attribué
     * On passe au slot suivant
     */
    toAttribute.foreach(slot => {
      var nextStaff = staffsToAttribute.headOption
      var rest = staffsToAttribute.tail
      var attributed = 0

      println("Attributing " + slot)

      while (attributed < slot.staffsRequired && nextStaff.isDefined) {
        val staff = nextStaff.get
        val constr = constraints.filter(c => c.appliesTo(staff, slot))
          .map(c => c.offerAssignation(staff, staffs, slot, attributions.toMap.mapValues(_.toSet)))

        if (!constr.exists(_.isEmpty)) {
          val staffs = constr.foldLeft(Set(staff))(_ union _)

          if (staffs.forall(staff => {
            isAble(staff, slot) && !isBusy(staff, slot) && hasEnoughRemainingTime(staff, slot)
          })) {

            staffs.foreach(staff => {
              attribute(slot, staff)
            })
            attributed += staffs.size
          }
        }

        nextStaff = rest.headOption

        if (rest.nonEmpty) {
          rest = rest.tail
        }
      }

      if (attributed < slot.staffsRequired) {
        notAttributed += slot
        println(s"PLANNER:: Cannot attribute slot ${slot.id} (${slot.task}). Only $attributed staffs found out of ${slot.staffsRequired}.")
      }
    })

    // Compute stats
    val stats = attributions.toList.flatMap { case (slot, staffs) => staffs.map(staff => staff -> slot) }.groupBy(_._1)
      .mapValues(_.map(_._2.timeSlot.duration.toDouble / 60D).sum)
      .values

    val (avg, std) = if (stats.nonEmpty) {
      val sum = stats.sum
      val cnt = stats.size
      val avg = sum.toDouble / cnt
      val variance = stats.map(x => math.pow(x - avg, 2)).sum / cnt
      val std = Math.sqrt(variance)

      (avg, std)
    } else { (0D, 0D)}

    attributions.flatMap {
      case (slot, set) => set.toList.map(staff => StaffAssignation(slot, staff))
    }.toList -> SchedulingResult(notAttributed.toList, avg, std)

  }
}
