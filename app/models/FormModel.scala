package models

import java.util.{Date, GregorianCalendar}
import javax.inject.Inject

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
  type Errors = List[String]

  def mapFormWithInput(form: Form, sourceObject: JsObject, inputJson: JsObject): (Errors, JsObject) =
    form.entries.foldLeft((List[String](), sourceObject))(
      (pair, entry) => (pair, entry) match {
        // We map the pair (list of errors, source js object) and the current entry to usable names
        case ((list: Errors, source: JsObject), (key: String, value: FormEntry)) =>
          if (inputJson.keys(key)) { // The input json contains the key
            if (value.decorator.validateInput(inputJson(key))) (list, source + (key, inputJson(key))) // the value is valid
            else (key :: list, source) // the value is invalid
          } else (list, source)
      }
    )

  /**
    * Lists the field from `form` that are not present in the `jsObject` object
    * @param form the form to check
    * @param jsObject the object to check
    * @param isMinor true if the `onlyIfMinor` fields should be considered as required
    * @return a list of pairs (field id, field name) of all the fields that are missing
    */
  def listMissingFields(form: Form, jsObject: JsObject, isMinor: Boolean): List[(String, String)] = {
    // Fonction qui collecte les champs de formulaires non remplis
    val folder = (list: List[(String, String)], entry: (String, FormEntry)) =>
      if (jsObject.keys(entry._1)) list
      else (entry._1, entry._2.label) :: list

    // Filtre pour ne garder que les champs de formulaires obligatoires
    var filter: ((String, FormEntry)) => Boolean = {
      case (_, entry) => entry.required || (entry.onlyIfMinor && isMinor)
    }

    form.entries.filter(filter).foldLeft(List[(String, String)]())(folder)
  }

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
    * @param ordering    a number specifying where the field should be rendered (lower value : displayed before)
    * @param decorator   additional info
    */
  case class FormEntry(label: String, helpText: String, required: Boolean = true, onlyIfMinor: Boolean = false, ordering: Int = Int.MaxValue, decorator: FormEntryDecorator)

  object FormEntry {
    implicit val formEntryFormat: OFormat[FormEntry] = Json.format[FormEntry]
  }

  /**
    * The decorator parser
    * Register your types here !
    */
  object FormEntryDecorator {
    implicit val converter: OFormat[FormEntryDecorator] = FormDecoratorFormat

    def apply(typeName: String, doc: JsObject): FormEntryDecorator = typeName match {
        case "string" => StringDecorator(doc("regex").as[String])
        case "integer" => IntegerDecorator(doc("minValue").as[Int], doc("maxValue").as[Int], doc("step").as[Int])
        case "date" => DateDecorator()
        case "boolean" => BooleanDecorator()
        case "choice" => ChoiceListDecorator(doc("choices").as[List[String]].toSet)
    }

    def unapply(arg: FormEntryDecorator): (String, JsObject) =
      arg match {
        case e: StringDecorator => ("string", Json.obj("regex" -> e.regex))
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
    */
  case class StringDecorator(regex: String) extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = input match {
      case (input: String) => input.matches(regex)
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

  object DateDecorator {
    object Int {
      // Use this to automatically convert the matched date strings to int when parsing the date
      def unapply(s : String) : Option[Int] = try {
        Some(s.toInt)
      } catch {
        case _ : java.lang.NumberFormatException => None
      }
    }

    val pattern = "^([0-9]{2})/([0-9]{2})/([0-9]{4})$".r

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

  case class DateDecorator() extends FormEntryDecorator {


    override def validateInput(input: Any): Boolean = input match {
      case (s: String) => DateDecorator.isValidDate(s)
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