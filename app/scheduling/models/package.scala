package scheduling

import java.sql.{Date, Time, Timestamp}

import akka.http.scaladsl.model.DateTime
import data.User
import play.api.libs.json.{Json, OWrites}
import scheduling.constraints.{BannedTaskConstraint, _}
import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

package object models {
  case class ScheduleProject(projectId: Option[Int], event: Int, projectTitle: String, maxTimePerStaff: Int, minBreakMinutes: Int, maxSameShiftType: Int)

  implicit val scheduleProjectFormat: OWrites[ScheduleProject] = Json.writes[ScheduleProject]

  private[scheduling] case class Task(taskId: Option[Int], projectId: Int, name: String, minAge: Int, minExperience: Int, taskType: Option[Int])

  case class TaskSlot(taskSlotId: Option[Int], taskId: Int, staffsRequired: Int, timeSlot: Period, versionId: Int = 0) {
    def assign(staff: User) = StaffAssignation(taskSlotId.get, staff.userId)
  }

  implicit val taskSlotFormat: OWrites[TaskSlot] = Json.writes[TaskSlot]

  private[models] case class StaffAssignation(taskSlot: Int, user: Int)


  private[models] class ScheduleProjects(tag: Tag) extends Table[ScheduleProject](tag, "schedule_projects") {
    def id = column[Int]("project_id", O.PrimaryKey, O.AutoInc)

    def event = column[Int]("event_id")

    def title = column[String]("project_title")

    def maxHoursPerStaff = column[Int]("max_daily_hours")
    def maxSameShiftDaily = column[Int]("max_same_shift_daily")

    def minBreakMinutes = column[Int]("min_break_minutes")

    def * = (id.?, event, title, maxHoursPerStaff, minBreakMinutes, maxSameShiftDaily).shaped <> (ScheduleProject.tupled, ScheduleProject.unapply)
  }

  val scheduleProjects = TableQuery[ScheduleProjects]

  private[models] class TaskTypes(tag: Tag) extends Table[String](tag, "task_types") {
    def id = column[Int]("task_type_id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("task_type_name")

    def * = name.shaped
  }

  val taskTypes = TableQuery[TaskTypes]

  private[models] class Tasks(tag: Tag) extends Table[Task](tag, "schedule_tasks") {
    def id = column[Int]("task_id", O.PrimaryKey, O.AutoInc)

    def projectId = column[Int]("project_id")

    def minAge = column[Int]("min_age")

    def minExperience = column[Int]("min_experience")

    def name = column[String]("name")

    def taskTypeId = column[Option[Int]]("task_type_id")

    def deleted = column[Boolean]("deleted", O.Default(true))

    def project = foreignKey("project", projectId, scheduleProjects)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, projectId, name, minAge, minExperience, taskTypeId).shaped <> (Task.tupled, Task.unapply)
  }

  val tasks = TableQuery[Tasks]


  private[models] class ScheduleVersions(tag: Tag) extends Table[ScheduleVersion](tag, "schedule_versions") {
    def id = column[Int]("version_id", O.PrimaryKey, O.AutoInc)

    def projectId = column[Int]("project_id")

    def generationTime = column[Timestamp]("generation_time", O.AutoInc) // AutoInc here means auto generated

    def versionTag = column[Option[String]]("tag")

    def visible = column[Boolean]("visible", O.Default(false))

    def project = foreignKey("project", projectId, scheduleProjects)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, projectId, generationTime.?, versionTag, visible).shaped <> (ScheduleVersion.tupled, ScheduleVersion.unapply)
  }

  val scheduleVersions = TableQuery[ScheduleVersions]

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

  private[models] class StaffCapabilities(tag: Tag) extends Table[(Int, Int, Int)](tag, "staffs_capabilities") {
    def eventId = column[Int]("event_id")
    def staffNumber = column[Int]("staff_number")
    def capabilityId = column[Int]("capability_id")

    def pKey = primaryKey("primary_key", (eventId, staffNumber, capabilityId))

    def * = (eventId, staffNumber, capabilityId).shaped
  }

  def capabilitiesMappingRequestForEvent(event: Int)(implicit ec: ExecutionContext): DBIOAction[Map[Int, List[String]], NoStream, Effect.Read] = {
    staffCapabilities.filter(_.eventId === event)
      .join(capabilities).on { case (staffCap, cap) => staffCap.capabilityId === cap.id }
      .map { case (staffCap, cap) => (staffCap.staffNumber, cap.name) }
      .result
      .map(res => res.groupMapReduce(_._1)(p => List(p._2))(_ ::: _).withDefaultValue(List.empty[String]))
  }

  val staffCapabilities = TableQuery[StaffCapabilities]

  private[models] abstract class PeriodTable[T](tag: Tag, tblName: String) extends Table[T](tag, tblName) {
    def day = column[Date]("day")

    def start = column[Time]("start")

    def end = column[Time]("end")

    def periodOrdering = (day, start, end)

    def period = (day, start, end).shaped <> (Period.apply, Period.unapply)
  }

  private[models] class TaskTimePartitions(tag: Tag) extends PeriodTable[TaskTimePartition](tag, "schedule_task_partitions") {
    def id = column[Int]("task_partition_id", O.PrimaryKey, O.AutoInc)

    def taskId = column[Int]("task_id")

    def splitIn = column[Int]("split_in")
    def staffsRequired = column[Int]("staffs_required")

    def offset = column[Option[Int]]("alternate_offset")

    def firstAltShift = column[PartitionRule.Value]("first_alternated_shift")
    def lastAltShift = column[PartitionRule.Value]("last_alternated_shift")
    def lastNormalShift = column[PartitionRule.Value]("last_normal_shift")

    def doAlternate = column[Boolean]("alternate_shifts")

    def * = (id.?, taskId, staffsRequired, splitIn, period, doAlternate, offset, firstAltShift, lastAltShift, lastNormalShift).shaped.<>(TaskTimePartition.tupled, TaskTimePartition.unapply)
  }

  val taskTimePartitions = TableQuery[TaskTimePartitions]

  private[models] class TaskSlots(tag: Tag) extends PeriodTable[TaskSlot](tag, "schedule_tasks_slots") {
    def id = column[Int]("task_slot_id", O.PrimaryKey, O.AutoInc)

    def taskId = column[Int]("task_id")

    def staffsRequired = column[Int]("staffs_required")

    def versionId = column[Int]("version_id")

    def task = foreignKey("task", taskId, tasks)(_.id, onDelete = ForeignKeyAction.Cascade)
    def version = foreignKey("version", versionId, scheduleVersions)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, taskId, staffsRequired, period, versionId).shaped <> (TaskSlot.tupled, TaskSlot.unapply)
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

  private[models] class BannedTaskConstraints(tag: Tag) extends ConstraintTable[BannedTaskConstraint](tag, "banned_task_constraints") {
    def staffId = column[Int]("staff_id")
    def taskId = column[Int]("task_id")

    def * = (constraintId.?, projectId, staffId, taskId).shaped <> (BannedTaskConstraint.tupled, BannedTaskConstraint.unapply)
  }

  val bannedTaskConstraints = TableQuery[BannedTaskConstraints]

  private[models] class BannedTaskTypeConstraints(tag: Tag) extends ConstraintTable[BannedTaskTypeConstraint](tag, "banned_task_types_constraints") {
    def staffId = column[Int]("staff_id")
    def taskTypeId = column[Int]("task_type_id")

    def * = (constraintId.?, projectId, staffId, taskTypeId).shaped <> (BannedTaskTypeConstraint.tupled, BannedTaskTypeConstraint.unapply)
  }

  val bannedTaskTypesConstraints = TableQuery[BannedTaskTypeConstraints]

  private[models] class FixedTaskConstraints(tag: Tag) extends ConstraintTable[FixedTaskConstraint](tag, "fixed_task_constraints") {
    def staffId = column[Int]("staff_id")
    def taskId = column[Int]("task_id")

    def day = column[Date]("day")
    def start = column[Time]("start")
    def end = column[Time]("end")

    def period = (day.?, start.?, end.?).shaped <> ({
      case (Some(d), s, e) => Some(Period(d, s.getOrElse(new Time(0)), e.getOrElse(new Time((23.hours + 59.minutes + 59.seconds).toMillis))))
      case _ => None
    }, (o: Option[Period]) => o match {
      case Some(Period(d, s, e)) => Some(Some(d), Some(s), Some(e))
      case None => Some(None, None, None)
    })

    def * = (constraintId.?, projectId, staffId, taskId, period).shaped <> (FixedTaskConstraint.tupled, FixedTaskConstraint.unapply)
  }

  val fixedTaskConstraints = TableQuery[FixedTaskConstraints]

  private[models] class UnavailableConstraints(tag: Tag) extends PeriodTable[UnavailableConstraint](tag, "unavailable_constraints") {
    def constraintId = column[Int]("constraint_id", O.PrimaryKey, O.AutoInc)
    def projectId = column[Int]("project_id")
    def staffId = column[Int]("staff_id")

    def * = (constraintId.?, projectId, staffId, period).shaped <> (UnavailableConstraint.tupled, UnavailableConstraint.unapply)
  }

  val unavailableConstraints = TableQuery[UnavailableConstraints]

  private[models] class AssociationConstraints(tag: Tag) extends ConstraintTable[AssociationConstraint](tag, "association_constraints") {
    def staff1 = column[Int]("staff_1")
    def staff2 = column[Int]("staff_2")
    def together = column[Boolean]("together")

    def * = (constraintId.?, projectId, staff1, staff2, together).shaped <> (AssociationConstraint.tupled, AssociationConstraint.unapply)
  }

  val associationConstraints = TableQuery[AssociationConstraints]
}
