import java.sql.{Date, Time, Timestamp}
import java.text.SimpleDateFormat
import java.util.{Calendar, GregorianCalendar}

import ch.japanimpact.api.events.events.SimpleEvent
import ch.japanimpact.auth.api.UserProfile
import data.Applications.{ApplicationComment, ApplicationState}
import play.api.libs.json._
import utils.EnumUtils

/**
 * @author Louis Vialar
 */
package object data {
  val AngularDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  val TimeFormat = new SimpleDateFormat("HH:mm")

  case class User(userId: Int, birthDate: Date) {
    def ageAt(date: java.util.Date): Int = {
      val eventDateCalendar = new GregorianCalendar()
      val birthDateCalendar = new GregorianCalendar()
      eventDateCalendar.setTime(date)
      birthDateCalendar.setTime(birthDate)

      val ageAtEndOfYear = eventDateCalendar.get(Calendar.YEAR) - birthDateCalendar.get(Calendar.YEAR)

      if (eventDateCalendar.get(Calendar.DAY_OF_YEAR) < birthDateCalendar.get(Calendar.DAY_OF_YEAR)) ageAtEndOfYear - 1
      else ageAtEndOfYear
    }
  }

  // case class Event(eventId: Option[Int], eventBegin: Date, name: String, mainForm: Option[Int], isActive: Boolean)

  implicit val tsFormat: Format[Timestamp] = new Format[Timestamp] {
    override def writes(o: Timestamp): JsValue = JsNumber(o.getTime)

    override def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsNumber(num) => JsSuccess(new Timestamp(num.toLongExact))
      case _ => JsError("invalid type");
    }
  }

  implicit val dateFormat: Format[Date] = new Format[Date] {
    override def writes(o: Date): JsValue = {
      JsString(o.toString)
    }

    override def reads(json: JsValue): JsResult[Date] = json match {
      case JsString(str) => try {
        JsSuccess(Date.valueOf(str))
      } catch {
        case _ =>
          val time = AngularDateFormat.parse(str).getTime
          JsSuccess(new Date(time))
      }
      case _ => JsError("invalid type")
    }
  }

  implicit val timeFormat: Format[Time] = new Format[Time] {
    override def writes(o: Time): JsValue = {
      JsString(TimeFormat.format(o))
    }

    private val pattern = raw"([0-9]{1,2}):([0-9]{1,2})".r

    override def reads(json: JsValue): JsResult[Time] = json match {
      case JsString(str) =>
        val timeString = if (str.count(_ == ':') >= 2) str else str + ":00"
        try {
          JsSuccess(Time.valueOf(timeString))
        } catch {
          case e: Exception => JsError(e.getMessage)
        }
      case _ => JsError("invalid type");
    }
  }


  object StaffLogType extends Enumeration {
    val Arrived, Left = Value
  }

  case class StaffArrivalLog(staffId: Int, eventId: Int, time: Option[Timestamp], action: StaffLogType.Value)

  implicit val staffLogTypeFormat: Format[StaffLogType.Value] = EnumUtils.format(StaffLogType)
  implicit val staffLogFormat: Format[StaffArrivalLog] = Json.format[StaffArrivalLog]

  object Forms {

    object FieldType extends Enumeration {
      val Text, LongText, Email, Date, Checkbox, Select, File, Image, Url = Value
    }

    case class Field(fieldId: Option[Int], pageId: Int, name: String, placeholder: String, helpText: Option[String], required: Boolean, `type`: FieldType.Value, ordering: Option[Int]) extends Ordered[Field] {
      override def compare(that: Field): Int = {
        val ord = ordering.getOrElse(0)
        val thatOrd = that.ordering.getOrElse(0)

        val cmp = ord.compare(thatOrd)

        if (cmp == 0) fieldId.get.compare(that.fieldId.get)
        else cmp
      }
    }

    case class Form(formId: Option[Int], eventId: Int, isMain: Boolean, internalName: String, name: String, shortDescription: String, description: String, maxAge: Int, minAge: Int, requiresStaff: Boolean, hidden: Boolean, closeDate: Option[Timestamp])

    case class FormPage(pageId: Option[Int], formId: Int, name: String, description: String, maxAge: Int, minAge: Int, ordering: Option[Int]) extends Ordered[FormPage] {
      override def compare(that: FormPage): Int = {
        val ord = ordering.getOrElse(0)
        val thatOrd = that.ordering.getOrElse(0)

        val cmp = ord.compare(thatOrd)

        if (cmp == 0) pageId.get.compare(that.pageId.get)
        else cmp
      }
    }

    case class FormReply(fieldId: Int, fieldValue: String)

    implicit val formReplyFormat: Format[FormReply] = Json.format[FormReply]

    implicit val typeFormat: Format[FieldType.Value] = EnumUtils.format(FieldType)
    implicit val formFormat: Format[Form] = Json.format[Form]
    implicit val fieldFormat: Format[Field] = Json.format[Field]
    implicit val formPageFormat: Format[FormPage] = Json.format[FormPage]
  }

  // implicit val eventFormat: Writes[Event] = Json.writes[Event]
  implicit val userFormat: Format[User] = Json.format[User]

  object Applications {

    object ApplicationState extends Enumeration {
      val Draft, Sent, Accepted, Refused, RequestedChanges = Value
    }

    implicit val applicationStateFormat: Format[ApplicationState.Value] = EnumUtils.format(ApplicationState)

    case class ApplicationComment(commentId: Option[Int], applicationId: Int, userId: Int, value: String, timestamp: Timestamp, userVisible: Boolean)

    implicit val applicationCommentFormat: Format[ApplicationComment] = Json.format[ApplicationComment]

  }

  object ReturnTypes {

    case class FilledPageField(field: data.Forms.Field, value: Option[String])

    case class FilledPage(page: Forms.FormPage, fields: Seq[FilledPageField])

    case class UserData(profile: UserProfile, birthDate: Date)

    case class ReducedUserData(firstName: String, lastName: String, email: String)

    object ReducedUserData {
      def apply(profile: UserProfile): ReducedUserData = ReducedUserData(profile.details.firstName, profile.details.lastName, profile.email)
    }

    case class ApplicationResult(user: UserData, state: ApplicationState.Value, content: Iterable[FilledPage])

    case class ApplicationListing(user: ReducedUserData, state: ApplicationState.Value, applicationId: Int)

    case class CommentWithAuthor(author: ReducedUserData, comment: ApplicationComment)

    case class StaffingHistory(staffNumber: Int, application: Int, event: SimpleEvent)

    case class ApplicationHistory(application: Int, state: ApplicationState.Value, form: Forms.Form, event: SimpleEvent)

    implicit val userDataFormat: OFormat[UserData] = Json.format[UserData]
    implicit val reducedUserDataFormat: OFormat[ReducedUserData] = Json.format[ReducedUserData]
    implicit val listingFormat: OFormat[ApplicationListing] = Json.format[ApplicationListing]
    implicit val fieldFormat: OFormat[FilledPageField] = Json.format[FilledPageField]
    implicit val pageFormat: OFormat[FilledPage] = Json.format[FilledPage]
    implicit val resultFormat: OFormat[ApplicationResult] = Json.format[ApplicationResult]
    implicit val commentFormat: OFormat[CommentWithAuthor] = Json.format[CommentWithAuthor]

    implicit def staffHistoryFormat: Writes[StaffingHistory] = Json.writes[StaffingHistory]

    implicit def applicationHistoryFormat: Writes[ApplicationHistory] = Json.writes[ApplicationHistory]
  }

  object Meals {
    case class Meal(mealId: Option[Int], eventId: Int, name: String, date: Option[Date])

    case class MealTaken(mealId: Int, userId: Int, timestamp: Option[Timestamp])

    implicit val mealFormat: OFormat[Meal] = Json.format[Meal]
    implicit val mealTakenFormat: OFormat[MealTaken] = Json.format[MealTaken]
  }
}
