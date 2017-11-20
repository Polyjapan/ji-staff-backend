package models

import java.util.Date
import javax.inject.Inject

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.{Filters, UpdateOptions}
import org.mongodb.scala.result.UpdateResult
import services.MongoDBService

import scala.concurrent.Future

/**
  * @author Louis Vialar
  */
object FormModel {

  class FormModel @Inject()(mongo: MongoDBService) {
    private def collection: MongoCollection[Document] = mongo.database.getCollection("forms")

    /**
      * Get the form corresponding to the given id
      *
      * @param id the id of the form to get
      * @return a future containing the form
      */
    def getForm(id: String): Future[Form] =
      collection.find(Filters.eq("id", id)).first.toFuture.map(Form(_))

    /**
      * Get all the forms in the system
      *
      * @return a future containing all the forms of the system
      */
    def getForms: Future[Seq[Form]] =
      collection.find.map(Form(_)).toFuture

    /**
      * Set (update or insert) a form in the database, based on its id
      *
      * @param form the form to set
      * @return a future containing the result of the update
      */
    def setForm(form: Form): Future[UpdateResult] =
      collection.replaceOne(Filters.eq("id", form.id), form.toDocument, UpdateOptions().upsert(true)).toFuture
  }

  /**
    * A form
    *
    * @param id      the id of the form (used to query it, it's a canonical readable string)
    * @param title   the title of the form (displayed at the top of the page)
    * @param entries all the fields in this form, format: `field-id` -> `field`
    */
  case class Form(id: String, title: String, entries: Map[String, FormEntry[Any]]) {
    /**
      * Returns a new form updating a given entry
      *
      * @param entry the entry (field id and field)
      * @return a new form with an updated entry
      */
    def +(entry: (String, FormEntry[Any])): Form = Form(id, title, entries + entry)

    def withTitle(t: String): Form = Form(id, t, entries)

    def withId(i: String): Form = Form(i, title, entries)

    def toDocument: Document = Document("id" -> id, "title" -> title, "questions" -> Document(entries mapValues (_.toDocument)))
  }

  object Form {
    def apply(document: Document): Form = {
      // We have to map the entries from a String -> Document map to a String -> FormEntry, using the
      // FormEntry.apply(Document) method
      Form(document("id"), document("title"), document[Map[String, Document]]("entries").mapValues(FormEntry(_)))
    }
  }

  /**
    * A field in a form
    *
    * @param entryType   the type of the field
    * @param label       the label of the field (kind of title)
    * @param helpText    a longer description for the field, when needed
    * @param required    true if this field is required
    * @param onlyIfMinor true if this field is required for users that won't be adults during the next edition
    * @param decorator   additional info depending on the field type
    * @tparam T the type of the field (again)
    */
  case class FormEntry[+T <: FormEntryDecorator](entryType: EntryType[T], label: String, helpText: String, required: Boolean, onlyIfMinor: Boolean, decorator: T) {
    def toDocument: Document = {
      Document(
        "entryType" -> entryType.getClass.getCanonicalName,
        "label" -> label,
        "helpText" -> helpText,
        "required" -> required,
        "onlyIfMinor" -> onlyIfMinor, // a parameter that is required only if the user is < 18 y.o. at the date of JI; required doesn't have to be "true"
        "decorator" -> decorator.toDocument
      )
    }
  }

  object FormEntry {
    /**
      * Build a `FormEntry` from a `Document`
      *
      * @param document the database document
      * @return the FormEntry corresponding to the given document
      */
    def apply(document: Document): FormEntry[Any] = {
      val entryType = Class.forName(document("entryType")).newInstance
      entryType match {
        case (e: EntryType[entryType.type]) =>
          new FormEntry[entryType.type](e, document("label"), document("helpText"), document("required"), document("onlyIfMinor"), e.decorator(document("decorator")))
      }
    }
  }

  /*
Internal logic
 */

  /**
    * A trait describing the additional data required to build or check the validity of the field
    */
  trait FormEntryDecorator {
    def toDocument: Document = Document()

    def validateInput(input: Any): Boolean = false
  }

  /**
    * The decorator for a string field
    *
    * @param minLength the minimal length of the string (inclusive)
    * @param maxLength the maximal length of the string (exclusive)
    */
  case class StringDecorator(minLength: Int, maxLength: Int) extends FormEntryDecorator {
    override def toDocument: Document = Document("minLength" -> minLength, "maxLength" -> maxLength)

    override def validateInput(input: Any): Boolean = {
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
    override def toDocument: Document = Document("minValue" -> minValue, "maxValue" -> maxValue, "step" -> step)

    override def validateInput(input: Any): Boolean = {
      case (input: Int) => input >= minValue && input < maxValue && (input - minValue) % step == 0
      case _ => false
    }
  }

  /**
    * The decorator for a Date field
    */
  case class DateDecorator() extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = {
      case (_: Date) => true // all dates are valid right
      case _ => false
    }
  }

  /**
    * The decorator for a Boolean field
    */
  case class BooleanDecorator() extends FormEntryDecorator {
    override def validateInput(input: Any): Boolean = {
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
    override def toDocument: Document = Document("choices" -> choices.toList)

    override def validateInput(input: Any): Boolean = {
      case (s: String) => choices(s) // default operation on choices is "contains"
      case _ => false
    }
  }

  /**
    * This class describes a possible type for an entry
    *
    * @param decorator a function building the decorator associated with this type
    * @tparam T the type of the decorator associated with this type
    */
  case class EntryType[T <: FormEntryDecorator](decorator: Document => T)

  case class StringEntryType() extends EntryType(doc => StringDecorator(doc("minLength"), doc("maxLength")))

  case class IntegerEntryType() extends EntryType(doc => IntegerDecorator(doc("minValue"), doc("maxValue"), doc("step")))

  case class DateEntryType() extends EntryType(_ => DateDecorator())

  case class BooleanEntryType() extends EntryType(_ => BooleanDecorator())

  case class ChoiceListEntryType() extends EntryType(doc => ChoiceListDecorator(doc.get[List[String]]("choices").get.toSet))

}