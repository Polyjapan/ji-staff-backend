package scheduling

import java.sql.{Date, Time}

import data.User
import slick.lifted.Tag

import slick.jdbc.MySQLProfile.api._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, MySQLProfile}

package object models {
  private[models] case class ScheduleProject(projectId: Option[Int], eventId: Int, projectTitle: String, maxTimePerStaff: Int)

  private[models] case class Task(taskId: Option[Int], projectId: Int, name: String, minAge: Int, minExperience: Int)

  private[models] case class TaskSlot(taskSlotId: Option[Int], taskId: Int, staffsRequired: Int, timeSlot: Period) {
    def assign(staff: User) = StaffAssignation(taskSlotId.get, staff.userId)
  }

  private[models] case class StaffAssignation(taskSlot: Int, user: Int)


  private[models] class ScheduleProjects(tag: Tag) extends Table[ScheduleProject](tag, "schedule_projects") {
    def id = column[Int]("project_id")

    def event = column[Int]("event_id")

    def title = column[String]("project_title")

    def maxHoursPerStaff = column[Int]("max_hours_per_staff")

    def * = (id.?, event, title, maxHoursPerStaff).shaped <> (ScheduleProject.tupled, ScheduleProject.unapply)
  }

  val scheduleProjects = TableQuery[ScheduleProjects]


  private[models] class Tasks(tag: Tag) extends Table[Task](tag, "schedule_tasks") {
    def id = column[Int]("task_id")

    def projectId = column[Int]("project_id")

    def minAge = column[Int]("min_age")

    def minExperience = column[Int]("min_experience")

    def name = column[String]("name")

    def project = foreignKey("project", projectId, scheduleProjects)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, projectId, name, minAge, minExperience).shaped <> (Task.tupled, Task.unapply)
  }

  val tasks = TableQuery[Tasks]

  private[models] class Capabilities(tag: Tag) extends Table[(Int, String)](tag, "schedule_capabilities") {
    def id = column[Int]("capability_id")

    def name = column[String]("name")

    def * = (id, name).shaped
  }

  val capabilities = TableQuery[Capabilities]

  private[models] class TasksCapabilities(tag: Tag) extends Table[(Int, Int)](tag, "schedule_capabilities") {
    def taskId = column[Int]("task_id")

    def capabilityId = column[Int]("capability_id")

    def task = foreignKey("task", taskId, tasks)(_.id, onDelete = ForeignKeyAction.Cascade)

    def capability = foreignKey("capability", capabilityId, capabilities)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (taskId, capabilityId).shaped
  }

  val taskCapabilities = TableQuery[TasksCapabilities]

  private[models] abstract class PeriodTable[T](tag: Tag, tblName: String) extends Table[T](tag, tblName) {
    def day = column[Date]("period_day")

    def start = column[Time]("period_start")

    def end = column[Time]("period_end")

    def period = (day, start, end).shaped <> (Period.tupled, Period.unapply)
  }

  private[models] class TaskTimePartitions(tag: Tag) extends PeriodTable[TaskTimePartition](tag, "schedule_tasks_partitions") {
    def id = column[Int]("task_partition_id")

    def taskId = column[Int]("task_id")

    def splitIn = column[Int]("split_in")

    def allowAlternate = column[Boolean]("allow_alternate")

    def firstStartsLater = column[Boolean]("first_starts_later")

    def firstEndsEarlier = column[Boolean]("first_ends_earlier")

    def lastEndsEarlier = column[Boolean]("last_ends_earlier")

    def lastEndsLater = column[Boolean]("last_ends_later")

    def task = foreignKey("task", taskId, tasks)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, taskId, splitIn, period, allowAlternate, firstStartsLater, firstEndsEarlier, lastEndsEarlier, lastEndsLater).shaped <> (TaskTimePartition.tupled, TaskTimePartition.unapply)
  }

  val taskTimePartitions = TableQuery[TaskTimePartitions]

  private[models] class TaskSlots(tag: Tag) extends PeriodTable[TaskSlot](tag, "schedule_tasks_slots") {
    def id = column[Int]("task_slot_id")

    def taskId = column[Int]("task_id")

    def staffsRequired = column[Int]("staffs_required")

    def task = foreignKey("task", taskId, tasks)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, task, staffsRequired, period).shaped <> (TaskSlot.tupled, TaskSlot.unapply)
  }

  val taskSlots = TableQuery[TaskSlots]


  private[models] class StaffsAssignation(tag: Tag) extends Table[StaffAssignation](tag, "schedule_staffs_assignation") {
    def taskSlotId = column[Int]("task_slot_id")

    def userId = column[Int]("user_id")

    def * = (taskSlotId, userId).shaped <> (StaffAssignation.tupled, StaffAssignation.unapply)
  }

  val staffsAssignation = TableQuery[StaffsAssignation]
}
