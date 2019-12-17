package scheduling

import javax.inject.{Inject, Singleton}
import jp.kobe_u.copris._
import jp.kobe_u.copris.dsl._
import scheduling.constraints.ScheduleConstraint
import scheduling.models.{SchedulingModel, TaskSlot, TaskTimePartition, taskSlots}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchedulingService @Inject()(schedulingModel: SchedulingModel)(implicit ec: ExecutionContext) {


  /**
   * This will take all the [[TaskTimePartition]] objects, produce the associated [[TaskSlot]], and push them to the database
   * @param project the project on which the operation should be done
   * @return
   */
  def buildSlots(project: Int): Future[_] = {
    schedulingModel.buildSlotsForProject(project)
  }

  def buildSchedule(project: Int): Future[_] = {
    schedulingModel.getScheduleData(project).map {
      case (proj, staffs, slots, constraints) => computePlanification(proj, staffs, slots, constraints)
    }.flatMap { case (list, _) => schedulingModel.pushSchedule(list)}
  }

  implicit def assignationAsVariable(assignation: StaffAssignation): Var = Var(assignation.taskSlot.id + "-by-" + assignation.user.user.userId)

  private def computePlanification(project: ScheduleProject, staffs: Seq[scheduling.Staff], slots: Iterable[scheduling.TaskSlot], constraints: Seq[ScheduleConstraint]): (List[StaffAssignation], Int) = {
    def planify(allowRecomputation: Boolean = true, recomputation: Int = 0): (List[StaffAssignation], Int) = {
      init

      // Create variables
      val assignations = staffs.flatMap(staff => slots.map(slot => StaffAssignation(slot, staff)))
      val variables = (assignations.map(ass => boolInt(Var(ass.toString))) zip assignations).toMap

      val constraintsApplied = constraints.flatMap(c => c.computeConstraint(variables))
      add(constraintsApplied: _*)

      for (staff <- staffs) {
        // Staffs cannot work more than max
        val sum = Add(slots.map(slot => Var(StaffAssignation(slot, staff).toString) * slot.timeSlot.duration))
        add(sum < project.maxTimePerStaff * 3600)

        // Staffs cannot work on tasks that are "too difficult" for them
        slots
          .filter(slot => slot.task.difficulties.nonEmpty)
          .filter(slot => slot.task.difficulties.exists(diff => !staff.capabilities.contains(diff)))
          .foreach(slot => add(slot.assign(staff) === 0))
      }

      for (slot <- slots) {
        // Each task has required number of staffs
        val sum = Add(staffs.map(staff => Var(StaffAssignation(slot, staff).toString)))

        if (!allowRecomputation || recomputation == 0)
          add(sum === slot.staffsRequired)
        else if (recomputation == 1)
          add(And(sum > 0, sum <= slot.staffsRequired))
        else add(sum <= slot.staffsRequired)

        // Each task only has qualified staffs
        staffs.foreach(staff => {
          if (staff.experience < slot.task.minExperience || staff.age < slot.task.minAge)
            add(slot.assign(staff) === 0)
        })

        // Staffs cannot work twice at the same time
        for (slot2 <- slots if slot2 != slot && slot.timeSlot.day == slot2.timeSlot.day) {
          // Both at the same time
          val s1 = slot.timeSlot
          val s2 = slot2.timeSlot

          if (s1.isOverlapping(s2)) {
            staffs.foreach(staff =>
              add( // no slot | slot 1 | slot 2 | both slots
                Not( // staff doesn't work on both slots (1 | 1 | 1 | 0)
                  And( // staff works on both slots (0 | 0 | 0 | 1)
                    slot.assign(staff) === 1, // staff works on slot 1
                    slot2.assign(staff) === 1) // staff works on slot 2
                )))
          }
        }
      }

      println(s"Project $project - Solving following constraints for computation $recomputation:")
      show

      if (find) solution.intValues.filter(p => p._2 > 0).map(p => variables(p._1)).toList -> recomputation
      else if (allowRecomputation && recomputation < 2) planify(allowRecomputation, recomputation + 1)._1 -> recomputation
      else List.empty[StaffAssignation] -> recomputation
    }

    planify()
  }
}
