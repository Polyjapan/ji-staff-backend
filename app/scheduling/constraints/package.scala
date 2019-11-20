package scheduling

import scheduling.models.StaffAssignation

package object constraints {

  import jp.kobe_u.copris.dsl._
  import jp.kobe_u.copris.{Constraint, Var}

  trait ScheduleConstraint {
    def computeConstraint(variables: Map[Var, StaffAssignation]): Iterable[Constraint]
  }

  case class FixedTaskConstraint(projectId: Int, staffId: Int, taskId: Int) extends ScheduleConstraint {
    override def computeConstraint(variables: Map[Var, StaffAssignation]): Iterable[Constraint] = {
      // Block other tasks
      variables.filter { case (variable, StaffAssignation(slot, user)) => slot _._2.slot.task != task).map(_._1 === 0)
      }
    }
}
