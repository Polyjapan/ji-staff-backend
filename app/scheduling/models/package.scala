package scheduling

import java.sql.{Date, Time}

import data.User
import play.api.libs.json.{Json, OWrites}
import scheduling.constraints._
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, MySQLProfile}

package object models {
  case class ScheduleProject(projectId: Option[Int], event: Int, projectTitle: String, maxTimePerStaff: Int)

  implicit val scheduleProjectFormat: OWrites[ScheduleProject] = Json.writes[ScheduleProject]

  private[scheduling] case class Task(taskId: Option[Int], projectId: Int, name: String, minAge: Int, minExperience: Int)

  private[models] case class TaskSlot(taskSlotId: Option[Int], taskId: Int, staffsRequired: Int, timeSlot: Period) {
    def assign(staff: User) = StaffAssignation(taskSlotId.get, staff.userId)
  }

  private[models] case class StaffAssignation(taskSlot: Int, user: Int)


  private[models] class ScheduleProjects(tag: Tag) extends Table[ScheduleProject](tag, "schedule_projects") {
    def id = column[Int]("project_id", O.PrimaryKey, O.AutoInc)

    def event = column[Int]("event_id")

    def title = column[String]("project_title")

    def maxHoursPerStaff = column[Int]("max_hours_per_staff")

    def * = (id.?, event, title, maxHoursPerStaff).shaped <> (ScheduleProject.tupled, ScheduleProject.unapply)
  }

  val scheduleProjects = TableQuery[ScheduleProjects]


  private[models] class Tasks(tag: Tag) extends Table[Task](tag, "schedule_tasks") {
    def id = column[Int]("task_id", O.PrimaryKey, O.AutoInc)

    def projectId = column[Int]("project_id")

    def minAge = column[Int]("min_age")

    def minExperience = column[Int]("min_experience")

    def name = column[String]("name")

    def project = foreignKey("project", projectId, scheduleProjects)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, projectId, name, minAge, minExperience).shaped <> (Task.tupled, Task.unapply)
  }

  val tasks = TableQuery[Tasks]

  private[models] class Capabilities(tag: Tag) extends Table[(Int, String)](tag, "schedule_capabilities") {
    def id = column[Int]("capability_id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def * = (id, name).shaped
  }

  val capabilities = TableQuery[Capabilities]

  private[models] class TasksCapabilities(tag: Tag) extends Table[(Int, Int)](tag, "task_capabilities") {
    def taskId = column[Int]("task_id")

    def capabilityId = column[Int]("capability_id")

    def pKey = primaryKey("primary_key", (taskId, capabilityId))

    def task = foreignKey("task", taskId, tasks)(_.id, onDelete = ForeignKeyAction.Cascade)

    def capability = foreignKey("capability", capabilityId, capabilities)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (taskId, capabilityId).shaped
  }

  val taskCapabilities = TableQuery[TasksCapabilities]

  private[models] abstract class PeriodTable[T](tag: Tag, tblName: String) extends Table[T](tag, tblName) {
    def day = column[Date]("day")

    def start = column[Time]("start")

    def end = column[Time]("end")

    def period = (day, start, end).shaped <> (Period.apply, Period.unapply)
  }

  private[models] class TaskTimePartitions(tag: Tag) extends PeriodTable[TaskTimePartition](tag, "schedule_task_partitions") {
    def id = column[Int]("task_partition_id", O.PrimaryKey, O.AutoInc)

    def taskId = column[Int]("task_id")

    def splitIn = column[Int]("split_in")
    def staffsRequired = column[Int]("staffs_required")

    def allowAlternate = column[Boolean]("allow_alternate")

    def firstStartsLater = column[Boolean]("first_starts_later")

    def firstEndsEarlier = column[Boolean]("first_ends_earlier")

    def lastEndsEarlier = column[Boolean]("last_ends_earlier")

    def lastEndsLater = column[Boolean]("last_ends_later")

    def task = foreignKey("task", taskId, tasks)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, taskId, staffsRequired, splitIn, period, allowAlternate, firstStartsLater, firstEndsEarlier, lastEndsEarlier, lastEndsLater).shaped.<>(TaskTimePartition.tupled, TaskTimePartition.unapply)
  }

  val taskTimePartitions = TableQuery[TaskTimePartitions]

  private[models] class TaskSlots(tag: Tag) extends PeriodTable[TaskSlot](tag, "schedule_tasks_slots") {
    def id = column[Int]("task_slot_id", O.PrimaryKey, O.AutoInc)

    def taskId = column[Int]("task_id")

    def staffsRequired = column[Int]("staffs_required")

    def task = foreignKey("task", taskId, tasks)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, taskId, staffsRequired, period).shaped <> (TaskSlot.tupled, TaskSlot.unapply)
  }

  val taskSlots = TableQuery[TaskSlots]


  private[models] class StaffsAssignation(tag: Tag) extends Table[StaffAssignation](tag, "schedule_staffs_assignation") {
    def taskSlotId = column[Int]("task_slot_id")

    def userId = column[Int]("user_id")

    def pKey = primaryKey("primary_key", (taskSlotId, userId))

    def * = (taskSlotId, userId).shaped <> (StaffAssignation.tupled, StaffAssignation.unapply)
  }

  val staffsAssignation = TableQuery[StaffsAssignation]

  private[models] abstract class ConstraintTable[T <: ScheduleConstraint](tag: Tag, tblName: String) extends Table[T](tag, tblName) {
    def constraintId = column[Int]("constraint_id", O.PrimaryKey, O.AutoInc)
    def projectId = column[Int]("project_id")

    def project = foreignKey("projects_fk", projectId, scheduleProjects)(_.id)
  }

  private[models] class FixedTaskConstraints(tag: Tag) extends ConstraintTable[FixedTaskConstraint](tag, "fixed_task_constraints") {
    def staffId = column[Int]("staff_id")
    def taskId = column[Int]("task_id")

    def * = (projectId, staffId, taskId).shaped <> (FixedTaskConstraint.tupled, FixedTaskConstraint.unapply)
  }

  val fixedTaskConstraints = TableQuery[FixedTaskConstraints]

  private[models] class FixedTaskSlotConstraints(tag: Tag) extends ConstraintTable[FixedTaskSlotConstraint](tag, "fixed_task_slot_constraints") {
    def staffId = column[Int]("staff_id")
    def slotId = column[Int]("task_slot_id")

    def * = (projectId, staffId, slotId).shaped <> (FixedTaskSlotConstraint.tupled, FixedTaskSlotConstraint.unapply)
  }

  val fixedTaskSlotConstraints = TableQuery[FixedTaskSlotConstraints]

  private[models] class UnavailableConstraints(tag: Tag) extends PeriodTable[UnavailableConstraint](tag, "unavailable_constraints") {
    def constraintId = column[Int]("constraint_id", O.PrimaryKey, O.AutoInc)
    def projectId = column[Int]("project_id")
    def staffId = column[Int]("staff_id")

    def * = (projectId, staffId, period).shaped <> (UnavailableConstraint.tupled, UnavailableConstraint.unapply)
  }

  val unavailableConstraints = TableQuery[UnavailableConstraints]

  private[models] class AssociationConstraints(tag: Tag) extends ConstraintTable[AssociationConstraint](tag, "association_constraints") {
    def staff1 = column[Int]("staff_1")
    def staff2 = column[Int]("staff_2")
    def together = column[Boolean]("together")

    def * = (projectId, staff1, staff2, together).shaped <> (AssociationConstraint.tupled, AssociationConstraint.unapply)
  }

  val associationConstraints = TableQuery[AssociationConstraints]
}
