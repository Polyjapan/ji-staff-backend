import java.sql.{Date, Time}
import java.time.LocalTime
import java.time.temporal.ChronoField

import play.api.libs.json.{Format, Json, OFormat, OWrites}
import scheduling.models.{TaskSlot, TaskTimePartition}

package object scheduling {
  import data._

  case class ScheduleProject(id: Int, event: Event, projectTitle: String, maxTimePerStaff: Int, minBreakMinutes: Int)

  implicit val scheduleProjectFormat: OWrites[ScheduleProject] = Json.writes[ScheduleProject]

  case class SchedulingResult(notFullSlots: List[TaskSlot], averageHoursPerStaff: Double, stdHoursPerStaff: Double)

  implicit val schedulingResultFormat: OWrites[SchedulingResult] = Json.writes[SchedulingResult]

  case class Period(day: Date, start: Time, end: Time) {
    private def timeToMinutes(t: Time) = t.toLocalTime.get(ChronoField.MINUTE_OF_DAY) // t.toInstant.get(ChronoField.SECOND_OF_DAY)

    lazy val timeStart: Int = timeToMinutes(start)
    lazy val timeEnd: Int = timeToMinutes(end)

    lazy val dayToString: String = day.toString

    lazy val duration: Int = timeEnd - timeStart

    def isOverlapping(other: Period): Boolean = isOverlappingWithBreakTime(other, 0)


    override def toString: String = day + " from " + start + " to " + end

    def isOverlappingWithBreakTime(other: Period, breakTime: Int): Boolean = {
      val (start1, end1) = (timeStart, timeEnd)
      val (start2, end2) = (other.timeStart, other.timeEnd)

      if (day != other.day) false
      else {
        val (first, second) = if (start1 < start2) (this, other) else (other, this)

        /*
        S1    S-----------E..
        S2         ---

        first.end > second.start


        S1    S-----------E..
        S2                     ---

        first.end < second.start


        S1    S-----------E..
        S2                 ---

        first.end > second.start


        S1    S-----------E..
        S2    ---

        first.start == second.start
        first.end > second.start
         */

        // if the second start before the end of the 1st it means overlap
        if (first.timeEnd + breakTime > second.timeStart) {
          true
        }
        else {
          false // pretty sure all other conditions are useless

          /*((s1.timeStart <= s2.timeStart && s1.timeEnd >= s2.timeStart) || // s1 starts before s2 but finishes after s2 starts
            (s2.timeStart <= s1.timeStart && s2.timeEnd >= s1.timeStart) || // s2 starts before s1 but finishes after s1 starts
            (s1.timeStart >= s2.timeStart && s1.timeEnd <= s2.timeEnd) || // s1 starts after s2 and finishes before s2
            (s2.timeStart >= s1.timeStart && s2.timeEnd <= s1.timeEnd)) // s2 starts after s1 and finishes befire s1*/
        }
      }
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

  case class ScheduleLine[LType](slot: models.TaskSlot, line: LType)

  case class ScheduleColumn[CType, LType](header: CType, content: List[ScheduleLine[LType]])

  case class ScheduleDay[CType, LType](day: java.sql.Date, startTime: Int, endTime: Int, schedule: List[ScheduleColumn[CType, LType]])

  case class StaffData(staffNumber: Int, staffName: String)

}
