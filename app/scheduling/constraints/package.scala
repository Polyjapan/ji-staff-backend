package scheduling

import java.sql.Date

package object constraints {

  trait ScheduleConstraint

  trait PreProcessConstraint extends ScheduleConstraint

  trait ProcessConstraint extends ScheduleConstraint {
    def appliesTo(staff: Staff, task: TaskSlot): Boolean

    def offerAssignation(offeredStaff: Staff, otherStaffs: Iterable[Staff], task: TaskSlot, assignations: Map[TaskSlot, Set[Staff]]): Set[Staff]
  }

  trait ResolutionConstraint extends ProcessConstraint {
    override def offerAssignation(offeredStaff: Staff, otherStaffs: Iterable[Staff], task: TaskSlot, assignations: Map[TaskSlot, Set[Staff]]): Set[Staff] = {
      if (isAssignationValid(offeredStaff, task, assignations)) Set(offeredStaff)
      else Set.empty
    }

    def isAssignationValid(staff: Staff, task: TaskSlot, assignations: Map[TaskSlot, Set[Staff]]): Boolean
  }

  case class BannedTaskConstraint(projectId: Int, staffId: Int, taskId: Int) extends ResolutionConstraint {
    override def isAssignationValid(staff: Staff, task: TaskSlot, assignations: Map[TaskSlot, Set[Staff]]): Boolean = appliesTo(staff, task)

    override def appliesTo(staff: Staff, task: TaskSlot): Boolean = staff.user.userId == staffId && task.task.id.get == taskId
  }

  case class FixedTaskSlotConstraint(projectId: Int, staffId: Int, slotId: Int) extends PreProcessConstraint

  case class UnavailableConstraint(projectId: Int, staffId: Int, period: Period) extends ResolutionConstraint {
    override def isAssignationValid(staff: Staff, task: TaskSlot, assignations: Map[TaskSlot, Set[Staff]]): Boolean =
      appliesTo(staff, task)

    override def appliesTo(staff: Staff, task: TaskSlot): Boolean = staffId == staff.user.userId && task.timeSlot.isOverlapping(period)
  }
  /**
   *
   * @param projectId
   * @param staff1
   * @param staff2
   * @param together if true, the constraint will put the staffs together - otherwise, it will ensure they are never together
   */
  case class AssociationConstraint(projectId: Int, staff1: Int, staff2: Int, together: Boolean) extends ProcessConstraint {
    override def appliesTo(staff: Staff, slot: TaskSlot): Boolean = staff.user.userId == staff1 || staff.user.userId == staff2

    override def offerAssignation(offeredStaff: Staff, otherStaffs: Iterable[Staff], task: TaskSlot, assignations: Map[TaskSlot, Set[Staff]]): Set[Staff] = {
      val staffId = if (offeredStaff.user.userId == staff1) staff2 else staff1
      val other = otherStaffs.find(s => s.user.userId == staffId)

      if (other.isEmpty) Set(offeredStaff)
      else if (together) Set(offeredStaff, other.get)
      else {
        // Avoid them to be together
        val overlappingSlots = assignations
          .filterKeys(k => k.task == task.task && k.timeSlot.isOverlapping(task.timeSlot))
          .flatMap(_._2)
          .toSet

        if (overlappingSlots.contains(other.get)) Set() // other already is in the shift
        else Set(offeredStaff)
      }
    }
  }

  object UnavailableConstraint {
    def tupled(tuple: (Int, Int, Period)) = tuple match {
      case (proj, staff, period) => UnavailableConstraint(proj, staff, period)
    }
    /**
     * Specify that a staff is unavailable during the whole day
     *
     * @param staff the unavailable staff
     * @param day   the day he is not available
     * @return the constraint
     */
    def day(projectId: Int, staff: Staff, day: Date): UnavailableConstraint = UnavailableConstraint(projectId, staff.user.userId, Period(day, 0, 24 * 60))

    /**
     * Specify that a staff is unavailable one day after a given time
     *
     * @param staff the unavailable staff
     * @param day   the day he is not available
     * @param start the time (in minutes after midnight) since when he is not available
     * @return the constraint
     */
    def after(projectId: Int, staff: Staff, day: Date, start: Int) = UnavailableConstraint(projectId, staff.user.userId, Period(day, start, 24 * 60))

    /**
     * Specify that a staff is unavailable one day before a given time
     *
     * @param staff the unavailable staff
     * @param day   the day he is not available
     * @param end   the time (in minutes after midnight) since when he is available
     * @return the constraint
     */
    def before(projectId: Int, staff: Staff, day: Date, end: Int) = UnavailableConstraint(projectId, staff.user.userId, Period(day, 0, end))
  }
}
