package scheduling

import java.sql.Date

package object constraints {

  import jp.kobe_u.copris._

  trait ScheduleConstraint {
    def computeConstraint(variables: Map[Var, StaffAssignation]): Iterable[Constraint]
  }

  case class FixedTaskConstraint(projectId: Int, staffId: Int, taskId: Int) extends ScheduleConstraint {
    override def computeConstraint(variables: Map[Var, StaffAssignation]): Iterable[Constraint] = {
      // Block other tasks
      variables.filter { case (_, StaffAssignation(slot, user)) => slot.task.id.get != taskId && user.user.userId == staffId }.map(_._1 === 0)
    }
  }

  case class FixedTaskSlotConstraint(projectId: Int, staffId: Int, slotId: Int) extends ScheduleConstraint {
    override def computeConstraint(variables: Map[Var, StaffAssignation]): Iterable[Constraint] = {
      variables filter { case (_, StaffAssignation(slot, user)) => slot.id == slotId && user.user.userId == staffId } map (_._1 === 1)
    }
  }


  case class UnavailableConstraint(projectId: Int, staffId: Int, period: Period) extends ScheduleConstraint {
    override def computeConstraint(variables: Map[Var, StaffAssignation]): Iterable[Constraint] = {
      variables.filter(p => p._2.user.user.userId == staffId && p._2.taskSlot.timeSlot.isOverlapping(period))
        .map(_._1 === 0) // These variables must be set to 0 (unavailability slot)
    }
  }
  /**
   *
   * @param projectId
   * @param staff1
   * @param staff2
   * @param together if true, the constraint will put the staffs together - otherwise, it will ensure they are never together
   */
  case class AssociationConstraint(projectId: Int, staff1: Int, staff2: Int, together: Boolean) extends ScheduleConstraint {
    override def computeConstraint(variables: Map[Var, StaffAssignation]): Iterable[Constraint] = {
      variables
        .groupBy(_._2.taskSlot) // for each taskslot
        .filter(_._1.staffsRequired >= 2) // lone tasks we don't care
        .flatMap(pair => {
          // Values are maps [var <> staff assignation] for the task
          val vars = pair._2.filter(p => {
            val staff = p._2.user.user.userId
            staff == staff1 || staff == staff2
          }).keys.toList // Keep only vars related to the two staffs

          if (vars.size < 2) None
          else if (together) Some(And(vars.head === 1, vars(1) === 1))
          else {
            Some(
              Not( // not both work on same task
                And( // both work on same task
                  vars.head === 1, // staff 1 works
                  vars(1) === 1) // staff 2 works
              ))
          }
        })
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
