import java.sql.{Date, Time}

import data.{Event, User}

package object scheduling {
  case class ScheduleProject(id: Int, event: Event, projectTitle: String, maxTimePerStaff: Int)

  case class Period(day: Date, start: Time, end: Time) {
    private def timeToSeconds(t: Time) = t.getTime.toInt // t.toInstant.get(ChronoField.SECOND_OF_DAY)

    lazy val timeStart: Int = timeToSeconds(start)
    lazy val timeEnd: Int = timeToSeconds(end)

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
      Period(day, new Time(start * 1000), new Time(end * 1000))
    }
  }

  case class Task(project: ScheduleProject, name: String, minAge: Int, minExperience: Int, difficulties: List[String])


  case class TaskSlot(id: Int, task: Task, staffsRequired: Int, timeSlot: Period) {
    def assign(staff: Staff) = StaffAssignation(this, staff)
  }

  case class StaffAssignation(taskSlot: TaskSlot, user: Staff)

  case class Staff(user: User, capabilities: List[String], experience: Int, age: Int)

}
