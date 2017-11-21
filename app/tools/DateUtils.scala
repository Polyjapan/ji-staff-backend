package tools

import java.util.{Date, GregorianCalendar}

import scala.util.matching.Regex

/**
  * @author Louis Vialar
  */
object DateUtils {
  object Int {
    // Use this to automatically convert the matched date strings to int when parsing the date
    def unapply(s : String) : Option[Int] = try {
      Some(s.toInt)
    } catch {
      case _ : java.lang.NumberFormatException => None
    }
  }

  val pattern: Regex = "^([0-9]{2})/([0-9]{2})/([0-9]{4})$".r

  def extractDate(s: String, yearOffset: Int = 0, monthOffset: Int = 0, dayOffset: Int = 0): Date = {
    val pattern(Int(day), Int(month), Int(year)) = s
    val calendar = new GregorianCalendar
    calendar.set(year + yearOffset, month + monthOffset, day + dayOffset, 0, 0, 0)
    calendar.getTime
  }



  def isValidDate(s: String): Boolean = {
    if (!s.matches(pattern.regex)) false
    else {
      val pattern(Int(day), Int(month), Int(year)) = s
      val days: Map[Int, Int] = Map(1 -> 31, 2 -> 29, 3 -> 31, 4 -> 30, 5 -> 31, 6 -> 30, 7 -> 31, 8 -> 31, 9 -> 30,
        10 -> 31, 11 -> 30, 12 -> 31)

      if (month <= 0 || month > 12) false
      else if (day <= 0 || day > days(month)) false
      else if (month == 2 && day == 29 && !isBissex(year)) false
      else true
    }
  }

  private def isBissex(year: Int): Boolean =
    (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0)
}
