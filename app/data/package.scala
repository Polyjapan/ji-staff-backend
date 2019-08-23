import java.sql.{Date, Timestamp}

import play.api.libs.json.{Format, JsError, JsNumber, JsResult, JsString, JsSuccess, JsValue, Json, Writes}

/**
 * @author Louis Vialar
 */
package object data {

  case class User(userId: Option[Int], email: String, firstName: String, lastName: String, birthDate: Date, phone: String, address: String)

  case class Event(eventId: Option[Int], eventBegin: Date, name: String, mainForm: Option[Int], isActive: Boolean)

  implicit val tsFormat: Writes[Timestamp] = (o: Timestamp) => JsNumber(o.getTime)
  implicit val dateFormat: Format[Date] = new Format[Date] {
    override def writes(o: Date): JsValue = {
      JsString(o.toString)
    }

    override def reads(json: JsValue): JsResult[Date] = json match {
      case JsString(str) => JsSuccess(Date.valueOf(str))
      case _ => JsError("invalid type")
    }
  }

  object Forms {

    object FieldType extends Enumeration {
      val Text, LongText, Email, Date, Checkbox, Select, File, Image, Url = Value
    }

    case class Field(fieldId: Option[Int], pageId: Int, name: String, label: String, helpText: String, required: Boolean, `type`: FieldType.Value, ordering: Option[Int]) extends Ordered[Field] {
      override def compare(that: Field): Int = {
        val ord = ordering.getOrElse(0)
        val thatOrd = that.ordering.getOrElse(0)

        val cmp = ord.compare(thatOrd)

        if (cmp == 0) fieldId.get.compare(that.fieldId.get)
        else cmp
      }
    }

    case class Form(formId: Option[Int], eventId: Int, internalName: String, name: String, shortDescription: String, description: String, maxAge: Int, minAge: Int, requiresStaff: Boolean, hidden: Boolean, closeDate: Option[Timestamp])

    case class FormPage(pageId: Option[Int], formId: Int, name: String, description: String, maxAge: Int, minAge: Int, ordering: Option[Int]) extends Ordered[FormPage] {
      override def compare(that: FormPage): Int = {
        val ord = ordering.getOrElse(0)
        val thatOrd = that.ordering.getOrElse(0)

        val cmp = ord.compare(thatOrd)

        if (cmp == 0) pageId.get.compare(that.pageId.get)
        else cmp
      }
    }

    implicit val typeFormat: Format[FieldType.Value] = EnumUtils.format(FieldType)
    implicit val formFormat: Writes[Form] = Json.writes[Form]
    implicit val fieldFormat: Format[Field] = Json.format[Field]
    implicit val formPageFormat: Format[FormPage] = Json.format[FormPage]
  }

  implicit val eventFormat: Writes[Event] = Json.writes[Event]
  implicit val userFormat: Format[User] = Json.format[User]

  object Applications {

    object ApplicationState extends Enumeration {
      val Draft, Sent, Accepted, Refused, RequestedChanges = Value
    }

    implicit val applicationStateFormat: Format[ApplicationState.Value] = EnumUtils.format(ApplicationState)


  }

}
