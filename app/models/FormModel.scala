package models

import java.util.Date
import javax.inject.Inject

import models.FormModel.FormEntryDecorator
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.ReadPreference
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.Cursor.{ErrorHandler, FailOnError}
import reactivemongo.api.commands.UpdateWriteResult

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.play.json._

/**
  * @author Louis Vialar
  */
object FormModel {

  class FormModel @Inject()(val reactiveMongoApi: ReactiveMongoApi, implicit val ec: ExecutionContext) {
    private def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("forms"))

    /**
      * Get the form corresponding to the given id
      *
      * @param id the id of the form to get
      * @return a future containing the form
      */
    def getForm(id: String): Future[Option[Form]] =
      collection.flatMap(_.find(Json.obj("id" -> id)).one[Form](ReadPreference.primary))

    /**
      * Get all the forms in the system
      *
      * @return a future containing all the forms of the system
      */
    def getForms: Future[Seq[Form]] =
      collection.flatMap(_.find(Json.obj()).cursor[Form](ReadPreference.primary).collect[Seq](-1, FailOnError[Seq[Form]]()))

    /**
      * Set (update or insert) a form in the database, based on its id
      *
      * @param form the form to set
      * @return a future containing the result of the update
      */
    def setForm(form: Form): Future[UpdateWriteResult] =
      collection.flatMap(_.update(Json.obj("id" -> form.id), form, upsert = true))
  }

  /**
    * A form
    *
    * @param id      the id of the form (used to query it, it's a canonical readable string)
    * @param title   the title of the form (displayed at the top of the page)
    * @param entries all the fields in this form, format: `field-id` -> `field`
    */
  case class Form(id: String, title: String, entries: Map[String, FormEntry]) {
    /**
      * Returns a new form updating a given entry
      *
      * @param entry the entry (field id and field)
      * @return a new form with an updated entry
      */
    def +(entry: (String, FormEntry)): Form = Form(id, title, entries + entry)

    def withTitle(t: String): Form = Form(id, t, entries)

    def withId(i: String): Form = Form(i, title, entries)
  }

  object Form {
    implicit val formReads: OFormat[Form] = Json.format[Form]
  }

  /**
    * A field in a form
    *
    * @param label       the label of the field (kind of title)
    * @param helpText    a longer description for the field, when needed
    * @param required    true if this field is required
    * @param onlyIfMinor true if this field is required for users that won't be adults during the next edition
    * @param decorator   additional info
    */
  case class FormEntry(label: String, helpText: String, required: Boolean, onlyIfMinor: Boolean, decorator: FormEntryDecorator)

  object FormEntry {
    implicit val formEntryFormat: OFormat[FormEntry] = Json.format[FormEntry]
  }

  /**
    * The decorator parser
    * Register your types here !
    */
  object FormEntryDecorator {
    def apply(typeName: String, doc: JsObject): FormEntryDecorator = typeName match {
        case "string" => StringDecorator(doc("minLength").as[Int], doc("maxLength").as[Int])
        case "integer" => IntegerDecorator(doc("minValue").as[Int], doc("maxValue").as[Int], doc("step").as[Int])
        case "date" => DateDecorator()
        case "boolean" => BooleanDecorator()
        case "choice" => ChoiceListDecorator(doc("choices").as[List[String]].toSet)
    }

    def unapply(arg: FormEntryDecorator): (String, JsObject) =
      arg match {
        case e: StringDecorator => ("string", Json.obj("minLength" -> e.minLength, "maxLength" -> e.maxLength))
        case e: IntegerDecorator => ("integer", Json.obj("minValue" -> e.minValue, "maxValue" -> e.maxValue, "step" -> e.step))
        case _: DateDecorator => ("date", Json.obj())
        case _: BooleanDecorator => ("boolean", Json.obj())
        case e: ChoiceListDecorator => ("choice", Json.obj("choices" -> e.choices.toList))
      }
  }


  /**
    * A trait describing the additional data required to build or check the validity of the field
    */
  abstract class FormEntryDecorator {
    def validateInput(input: Any): Boolean = false
  }

  implicit object FormDecoratorFormat extends OFormat[FormEntryDecorator] {
    override def reads(json: JsValue): JsResult[FormEntryDecorator] = json match {
      case o: JsObject => JsSuccess(FormEntryDecorator.apply(o("type").as[String], o("data").as[JsObject]))
      case _ => JsError()
    }

    override def writes(o: FormEntryDecorator): JsObject = FormEntryDecorator.unapply(o) match {
      case (t, obj) => Json.obj("type" -> t, "data" -> obj)
    }
  }


  /**
    * The decorator for a string field
    *
    * @param minLength the minimal length of the string (inclusive)
    * @param maxLength the maximal length of the string (exclusive)
    */
  case class StringDecorator(minLength: Int, maxLength: Int) extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = input match {
      case (input: String) => input.length >= minLength && input.length < maxLength
      case _ => false
    }
  }

  /**
    * The decorator for an integer field
    *
    * @param minValue the minimal value of the integer (inclusive)
    * @param maxValue the maximal value of the integer (exclusive)
    * @param step     the step between two allowed values
    */
  case class IntegerDecorator(minValue: Int, maxValue: Int, step: Int) extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = input match {
      case (input: Int) => input >= minValue && input < maxValue && (input - minValue) % step == 0
      case _ => false
    }
  }

  /**
    * The decorator for a Date field
    */
  case class DateDecorator() extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = input match {
      case (_: Date) => true // all dates are valid right
      case _ => false
    }
  }

  /**
    * The decorator for a Boolean field
    */
  case class BooleanDecorator() extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = input match {
      case (_: Boolean) => true // all dates are valid right
      case _ => false
    }
  }

  /**
    * The decorator for a Choice List
    *
    * @param choices all the possible choices the user can pick
    */
  case class ChoiceListDecorator(choices: Set[String]) extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = input match {
      case (s: String) => choices(s) // default operation on choices is "contains"
      case _ => false
    }
  }


}