
import java.sql.{Date, Time, Timestamp}
import data.Applications.ApplicationComment
import data.Meals.{Meal, MealTaken}
import slick.jdbc.MySQLProfile.api._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, MySQLProfile}
import utils.EnumUtils
import data.{Forms, StaffArrivalLog, _}

import java.time.LocalDate


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

    def birthDate = column[LocalDate]("birth_date")

    def * = (userId, birthDate).shaped <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  implicit val typeMap: JdbcType[Forms.FieldType.Value] with BaseTypedType[Forms.FieldType.Value] = EnumUtils.methodMap(Forms.FieldType, MySQLProfile)

  private[models] class Fields(tag: Tag) extends Table[Forms.Field](tag, "fields") {
    def fieldId = column[Int]("field_id", O.PrimaryKey, O.AutoInc)

    def pageId = column[Int]("form_page_id")

    def ordering = column[Option[Int]]("ordering")

    def name = column[String]("name")

    def placeholder = column[String]("placeholder")

    def helpText = column[Option[String]]("help_text")

    def required = column[Boolean]("required")

    def `type` = column[Forms.FieldType.Value]("type")

    def * = (fieldId.?, pageId, name, placeholder, helpText, required, `type`, ordering).shaped <> (Forms.Field.tupled, Forms.Field.unapply)
  }

  val fields = TableQuery[Fields]

  private[models] class FieldsAdditional(tag: Tag) extends Table[(Int, String)](tag, "fields_additional") {
    def fieldId = column[Int]("field_id")

    def ordering = column[Int]("ordering")

    def value = column[String]("value")

    def pkey = primaryKey("primaryKey", (fieldId, value))

    def * = (ordering, value).shaped
  }

  val fieldsAdditional = TableQuery[FieldsAdditional]

  private[models] class Forms(tag: Tag) extends Table[Forms.Form](tag, "forms") {
    def formId = column[Int]("form_id", O.PrimaryKey, O.AutoInc)

    def eventId = column[Int]("event")

    def isMain = column[Boolean]("is_main", O.Default(false))

    def internalName = column[String]("internal_name")

    def name = column[String]("name")

    def description = column[String]("description")

    def shortDescription = column[String]("short_description")

    def minAge = column[Int]("min_age")

    def maxAge = column[Int]("max_age")

    def staffOnly = column[Boolean]("requires_staff")

    def hidden = column[Boolean]("hidden")

    def closeDate = column[Option[Timestamp]]("close_date")

    def * = (formId.?, eventId, isMain, internalName, name, description, shortDescription, maxAge, minAge, staffOnly, hidden, closeDate)
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

    def * = (formPageId.?, formId, name, description, maxAge, minAge, ordering)
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

  private[models] class ApplicationsComments(tag: Tag) extends Table[ApplicationComment](tag, "applications_comments") {
    def commentId = column[Int]("application_comment_id", O.PrimaryKey, O.AutoInc)

    def applicationId = column[Int]("application_id")

    def userId = column[Int]("user_id")

    def value = column[String]("value")

    def timestamp = column[Timestamp]("timestamp")

    def userVisible = column[Boolean]("user_visible")

    def * = (commentId.?, applicationId, userId, value, timestamp, userVisible).shaped <> (ApplicationComment.tupled, ApplicationComment.unapply)
  }

  val applicationsComments = TableQuery[ApplicationsComments]


  private[models] class Staffs(tag: Tag) extends Table[(Int, Int, Int)](tag, "staffs") {
    def eventId = column[Int]("event_id")

    def staffNumber = column[Int]("staff_number")

    def userId = column[Int]("user_id")

    def staffLevel = column[Int]("staff_level", O.Default(0))

    def * = (eventId, staffNumber, userId).shaped
  }

  val staffs = TableQuery[Staffs]

  implicit val staffLogTypeMap: JdbcType[StaffLogType.Value] with BaseTypedType[StaffLogType.Value] = EnumUtils.methodMap(StaffLogType, MySQLProfile)

  private[models] class StaffArrivalLogs(tag: Tag) extends Table[StaffArrivalLog](tag, "staff_arrivals_logs") {
    def eventId = column[Int]("event_id")

    def staffId = column[Int]("staff_id")

    def action = column[StaffLogType.Value]("action")

    def time = column[Option[Timestamp]]("time")

    def * = (staffId, eventId, time, action).shaped <> (StaffArrivalLog.tupled, StaffArrivalLog.unapply)
  }

  val staffArrivalLogs = TableQuery[StaffArrivalLogs]

  private[models] class Meals(tag: Tag) extends Table[Meal](tag, "meals") {
    def eventId = column[Int]("event_id")

    def mealId = column[Int]("meal_id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("meal_name")

    def date = column[Option[Date]]("meal_date")

    def * = (mealId.?, eventId, name, date).shaped <> (Meal.tupled, Meal.unapply)
  }

  val meals = TableQuery[Meals]

  private[models] class MealsTaken(tag: Tag) extends Table[MealTaken](tag, "meals_taken") {
    def userId = column[Int]("user_id")

    def mealId = column[Int]("meal_id")

    def date = column[Option[Timestamp]]("timestamp")

    def * = (mealId, userId, date).shaped <> (MealTaken.tupled, MealTaken.unapply)
  }

  val mealsTaken = TableQuery[MealsTaken]

  private[models] class AdminFoodParticularities(tag: Tag) extends Table[(Int, String)](tag, "admin_food_particularities") {
    def userId = column[Int]("admin_id", O.PrimaryKey)

    def foodParticularities = column[String]("food_particularities")

    def * = (userId, foodParticularities).shaped
  }

  val adminFoodParticularities = TableQuery[AdminFoodParticularities]

  private[models] class StaffFoodParticularities(tag: Tag) extends Table[(Int, Int)](tag, "staff_food_particularities") {
    def eventId = column[Int]("event_id", O.PrimaryKey)

    def particularitiesField = column[Int]("particularities_field_id")

    def * = (eventId, particularitiesField).shaped
  }

  val staffFoodParticularities = TableQuery[StaffFoodParticularities]

}
