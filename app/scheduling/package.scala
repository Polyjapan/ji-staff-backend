import java.sql.{Date, Time}
import java.time.LocalTime
import java.time.temporal.ChronoField

import play.api.libs.json.{Format, Json, OFormat, OWrites}
import scheduling.models.TaskTimePartition

package object scheduling {
  import data._

  case class ScheduleProject(id: Int, event: Event, projectTitle: String, maxTimePerStaff: Int)

  implicit val scheduleProjectFormat: OWrites[ScheduleProject] = Json.writes[ScheduleProject]

  case class Period(day: Date, start: Time, end: Time) {
    private def timeToMinutes(t: Time) = t.toLocalTime.get(ChronoField.MINUTE_OF_DAY) // t.toInstant.get(ChronoField.SECOND_OF_DAY)

    lazy val timeStart: Int = timeToMinutes(start)
    lazy val timeEnd: Int = timeToMinutes(end)

    lazy val dayToString: String = day.toString

    lazy val duration: Int = timeEnd - timeStart

    def isOverlapping(other: Period): Boolean = {
      val s1 = this
      val s2 = other

      day == other.day &&
        ((s1.timeStart <= s2.timeStart && s1.timeEnd >= s2.timeStart) || // s1 starts before s2 but finishes after s2 starts
          (s2.timeStart <= s1.timeStart && s2.timeEnd >= s1.timeStart) || // s2 starts before s1 but finishes after s1 starts
          (s1.timeStart >= s2.timeStart && s1.timeEnd <= s2.timeEnd) || // s1 starts after s2 and finishes before s2
          (s2.timeStart >= s1.timeStart && s2.timeEnd <= s1.timeEnd)) // s2 starts after s1 and finishes befire s1
    }
  }

  object Period {
    def apply(day: Date, start: Int, end: Int): Period = {
      val startTime = Time.valueOf(LocalTime.of(start / 60, start % 60))
      val endTime = Time.valueOf(LocalTime.of(end / 60, end % 60))

      Period(day, startTime, endTime)
    }

    def apply(tuple: (Date, Time, Time)): Period = {
      val (d, s, e) = tuple
      Period(d, s, e)
    }
  }

  implicit val periodFormat: Format[Period] = Json.format[Period]


  case class Task(id: Option[Int], projectId: Int, name: String, minAge: Int, minExperience: Int, difficulties: List[String])

  implicit val taskFormat: OFormat[Task] = Json.format[Task]

  case class TaskSlot(id: Int, task: Task, staffsRequired: Int, timeSlot: Period) {
    def assign(staff: Staff) = StaffAssignation(this, staff)
  }

  implicit val taskSlotFormat: OWrites[TaskSlot] = Json.writes[TaskSlot]

  case class StaffAssignation(taskSlot: TaskSlot, user: Staff)

  case class Staff(user: User, capabilities: List[String], experience: Int, age: Int)

  implicit val taskTimePartitionFormat: OFormat[TaskTimePartition] = Json.format[TaskTimePartition]

}
