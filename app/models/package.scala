
import java.sql.{Date, Timestamp}

import slick.jdbc.MySQLProfile.api._
import data.{Forms, _}
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, MySQLProfile}

/**
 * @author Louis Vialar
 */
package object models {

  private[models] class AuthorizedApps(tag: Tag) extends Table[(String, String)](tag, "authorized_apps") {
    def appName = column[String]("app_name", O.PrimaryKey)

    def appKey = column[String]("app_key", O.Unique)

    def * = (appName, appKey).shaped
  }

  val apps = TableQuery[AuthorizedApps]

  private[models] class Users(tag: Tag) extends Table[User](tag, "users") {
    def userId = column[Int]("user_id", O.PrimaryKey)

    def email = column[String]("email")

    def firstName = column[String]("first_name")

    def lastName = column[String]("last_name")

    def birthDate = column[Date]("birth_date")

    def phone = column[String]("phone")

    def address = column[String]("address")

    def * = (userId, email, firstName, lastName, birthDate, phone, address).shaped <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  private[models] class Events(tag: Tag) extends Table[Event](tag, "events") {
    def eventId = column[Int]("event_id", O.PrimaryKey, O.AutoInc)

    def eventBegin = column[Date]("event_begin")

    def name = column[String]("name")

    def mainForm = column[Option[Int]]("main_form", O.Default(null))

    def isActive = column[Boolean]("is_active")

    def * = (eventId.?, eventBegin, name, mainForm, isActive).shaped <> (Event.tupled, Event.unapply)
  }

  val events = TableQuery[Events]

  implicit val typeMap: JdbcType[Forms.FieldType.Value] with BaseTypedType[Forms.FieldType.Value] = EnumUtils.methodMap(Forms.FieldType, MySQLProfile)

  private[models] class Fields(tag: Tag) extends Table[Forms.Field](tag, "fields") {
    def fieldId = column[Int]("field_id", O.PrimaryKey, O.AutoInc)

    def pageId = column[Int]("form_page_id")

    def ordering = column[Option[Int]]("ordering")

    def name = column[String]("name")

    def label = column[String]("label")

    def helpText = column[String]("help_text")

    def required = column[Boolean]("required")

    def `type` = column[Forms.FieldType.Value]("type")

    def * = (fieldId.?, pageId, name, label, helpText, required, `type`, ordering).shaped <> (Forms.Field.tupled, Forms.Field.unapply)
  }

  val fields = TableQuery[Fields]

  private[models] class FieldsAdditional(tag: Tag) extends Table[(String, String)](tag, "fields_additional") {
    def fieldId = column[Int]("field_id")

    def key = column[String]("key")

    def value = column[String]("value")

    def * = (key, value).shaped
  }

  val fieldsAdditional = TableQuery[FieldsAdditional]

  private[models] class Forms(tag: Tag) extends Table[Forms.Form](tag, "forms") {
    def formId = column[Int]("form_id", O.PrimaryKey, O.AutoInc)

    def eventId = column[Int]("event")

    def internalName = column[String]("internal_name")

    def name = column[String]("name")

    def description = column[String]("description")

    def shortDescription = column[String]("short_description")

    def minAge = column[Int]("min_age")

    def maxAge = column[Int]("max_age")

    def staffOnly = column[Boolean]("requires_staff")

    def hidden = column[Boolean]("hidden")

    def closeDate = column[Option[Timestamp]]("close_date")

    def * = (formId.?, eventId, internalName, name, description, shortDescription, minAge, maxAge, staffOnly, hidden, closeDate)
      .shaped <> (Forms.Form.tupled, Forms.Form.unapply)
  }

  val forms = TableQuery[Forms]

  private[models] class FormPages(tag: Tag) extends Table[Forms.FormPage](tag, "form_pages") {
    def formPageId = column[Int]("form_page_id", O.PrimaryKey, O.AutoInc)

    def formId = column[Int]("form")

    def name = column[String]("name")

    def description = column[String]("description")

    def minAge = column[Int]("min_age")

    def maxAge = column[Int]("max_age")

    def ordering = column[Option[Int]]("ordering")

    def * = (formPageId.?, formId, name, description, minAge, maxAge, ordering)
      .shaped <> (Forms.FormPage.tupled, Forms.FormPage.unapply)
  }

  val pages = TableQuery[FormPages]

  implicit val stateMap: JdbcType[Applications.ApplicationState.Value] with BaseTypedType[Applications.ApplicationState.Value] = EnumUtils.methodMap(Applications.ApplicationState, MySQLProfile)

  private[models] class Applications(tag: Tag) extends Table[(Option[Int], Int, Int, Applications.ApplicationState.Value)](tag, "applications") {
    def applicationId = column[Int]("application_id", O.PrimaryKey, O.AutoInc)

    def userId = column[Int]("user_id")

    def formId = column[Int]("form_id")

    def state = column[Applications.ApplicationState.Value]("state")

    def * = (applicationId.?, userId, formId, state).shaped
  }

  val applications = TableQuery[Applications]

  private[models] class ApplicationsContents(tag: Tag) extends Table[(Int, Int, String)](tag, "applications_contents") {
    def applicationId = column[Int]("application_id")

    def fieldId = column[Int]("field_id")

    def value = column[String]("value")

    def pk = primaryKey("pk", (applicationId, fieldId))


    def * = (applicationId, fieldId, value).shaped
  }

  val applicationsContents = TableQuery[ApplicationsContents]

  private[models] class ApplicationsComments(tag: Tag) extends Table[(Option[Int], Int, Int, String, Timestamp, Boolean)](tag, "applications_comments") {
    def commentId = column[Int]("application_comment_id", O.PrimaryKey, O.AutoInc)

    def applicationId = column[Int]("application_id")

    def userId = column[Int]("user_id")

    def value = column[String]("value")

    def timestamp = column[Timestamp]("timestamp")

    def userVisible = column[Boolean]("user_visible")

    def * = (commentId.?, applicationId, userId, value, timestamp, userVisible).shaped
  }

  val applicationsComments = TableQuery[ApplicationsComments]


  private[models] class Staffs(tag: Tag) extends Table[(Int, Int)](tag, "staffs") {
    def eventId = column[Int]("event_id")

    def staffNumber = column[Int]("staff_number")

    def userId = column[Int]("user_id")


    def * = (staffNumber, userId).shaped
  }

  val staffs = TableQuery[Staffs]


}
