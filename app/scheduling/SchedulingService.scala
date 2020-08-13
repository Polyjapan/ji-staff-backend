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

    val amt = staffs.size.toDouble
    val difficultyRarityScore: Map[String, Double] = staffs.flatMap(s => s.capabilities)
      .groupBy(a => a)
      .view
      .mapValues(_.size / amt)
      .mapValues(avail => 1 / avail)
      .toMap
      .withDefaultValue(1)

    val experienceRarityScore: Map[Int, Double] = staffs.map(staff => staff.experience)
      .foldLeft(Map[Int, Int]().withDefaultValue(0)) { (map, exp) => (1 to exp).foldLeft(map) {
        (map, exp) => map.updated(exp, map(exp) + 1)
      } }
      .view
      .mapValues(_ / amt)
      .mapValues(avail => 1 / avail)
      .toMap
      .withDefaultValue(1)

    println("Difficulties rarity score:")
    println(difficultyRarityScore)
    println("Experience rarity score:")
    println(experienceRarityScore)

    def taskRarityScore(task: Task) = {
      (task.difficulties.map(difficultyRarityScore).product * experienceRarityScore(task.minExperience) * 1000).toInt
    }

    implicit val slotsOrdering: Ordering[TaskSlot] = (x, y) => {
      val a = periodsOrdering.compare(x.timeSlot, y.timeSlot)
      val b = taskRarityScore(x.task) - taskRarityScore(y.task)

      if (b == 0) {
        if (a == 0) {
          x.id - y.id
        } else a // desc ordering of difficulties
      } else -b
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

    def countSameShiftTypesOnDay(staff: Staff, day: Date, shiftType: Int): Int = attributionsFor(staff).toList.filter(_.timeSlot.day == day).flatMap(_.task.taskType).count(_ == shiftType)

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

    def countAttributed(slot: TaskSlot): Int = {
      if (!attributions.contains(slot)) {
        0
      } else {
        attributions(slot).size
      }
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

    def hasEnoughRemainingTasks(staff: Staff, slot: TaskSlot): Boolean = {
      val sameTasks = slot.task.taskType.map(tpe => countSameShiftTypesOnDay(staff, slot.timeSlot.day, tpe) + 1).getOrElse(0)
      sameTasks <= project.maxSameShiftType
    }


    // Pre-attributed slots
    val staffsMap = staffs.map(s => (s.user.userId -> s)).toMap

    preConstraints.foreach {
      case FixedTaskConstraint(_, _, staffId, taskId, period) =>
        // This constraints attributes the longest possible stream of slots to someone in a task

        val staff = staffsMap(staffId)
        val eligibleSlots = slots
          .filter(_.task.id.get == taskId)
          .filter(task => countAttributed(task) < task.staffsRequired)


        val (takenSlots, dur) = longestNonOverlapping(eligibleSlots.toList, period)
        takenSlots.foreach(slot => attribute(slot, staff))
      case _ =>
    }


    val toAttribute = slots.toList.sorted

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
      var attributed = countAttributed(slot)

      if (attributed < slot.staffsRequired)
        println("Attributing " + slot + " (score " + taskRarityScore(slot.task) + ")")

      var issues = List.empty[String]
      var allowMoreTasks = false

      while (attributed < slot.staffsRequired && nextStaff.isDefined) {
        val staff = nextStaff.get
        val appliableConstraints = constraints.filter(c => c.appliesTo(staff, slot))
        val constr = appliableConstraints
          .map(c => c.offerAssignation(staff, staffs, slot, attributions.view.mapValues(_.toSet).toMap))

        if (!constr.exists(_.isEmpty)) {
          val staffs = constr.foldLeft(Set(staff))(_ union _)
          val unavailable = staffs.filterNot(staff => {
            isAble(staff, slot) && !isBusy(staff, slot) && hasEnoughRemainingTime(staff, slot) && (allowMoreTasks || hasEnoughRemainingTasks(staff, slot))
          })

          if (unavailable.isEmpty && (staffs.size + attributed) <= slot.staffsRequired) {
            staffs.foreach(staff => {
              attribute(slot, staff)
            })
            attributed += staffs.size
          } else {
            issues = unavailable.filter(s => isAble(s, slot)).map(s => {
              val cause =
                if (isBusy(s, slot)) "is busy"
                else if (!hasEnoughRemainingTime(s, slot)) "doesn't have time"
                else if (!hasEnoughRemainingTasks(s, slot)) "did that too much"
                else "???"

              ".. Staff " + s + " (when going through " + staff.user.userId + "): " + cause
            }).toList ::: issues
          }
        } else {
          issues = constr.zip(appliableConstraints).filter(_._1.isEmpty).map(_._2.toString).map(s => " .. Constraint fail " + s).toList ::: issues
        }

        nextStaff = rest.headOption

        if (rest.nonEmpty) {
          rest = rest.tail
        }

        if (nextStaff.isEmpty && !allowMoreTasks) {
          // Second pass system
          println(s"PLANNER:: Cannot attribute slot ${slot.id} (${slot.task}) after first pass. Only $attributed staffs found out of ${slot.staffsRequired}. Retrying without max jobs limit.")

          allowMoreTasks = true
          nextStaff = staffsToAttribute.headOption
          rest = staffsToAttribute.tail
        }
      }

      if (attributed < slot.staffsRequired) {
        notAttributed += slot
        println(s"PLANNER:: Cannot attribute slot ${slot.id} (${slot.task}). Only $attributed staffs found out of ${slot.staffsRequired}.")
        issues.foreach(println)
      }
    })

    // Compute stats
    val stats = attributions.toList.flatMap { case (slot, staffs) => staffs.map(staff => staff -> slot) }.groupBy(_._1)
      .map(pair => pair._2.map(_._2.timeSlot.duration.toDouble / 60D).sum)

    val (avg, std) = if (stats.nonEmpty) {
      val sum = stats.sum
      val cnt = stats.size
      val avg = sum.toDouble / cnt
      val variance = stats.map(x => math.pow(x - avg, 2)).sum / cnt
      val std = Math.sqrt(variance)

      (avg, std)
    } else {
      (0D, 0D)
    }

    attributions.flatMap {
      case (slot, set) => set.toList.map(staff => StaffAssignation(slot, staff))
    }.toList -> SchedulingResult(notAttributed.toList, avg, std)

  }
}
