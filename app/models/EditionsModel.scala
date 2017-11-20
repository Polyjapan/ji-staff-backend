package models

import java.util.Date
import javax.inject.Inject

import org.mongodb.scala.model.{Filters, UpdateOptions}
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Document, MongoCollection}
import services.MongoDBService

import scala.concurrent.Future

/**
  * An edition is a set of parameters about a Japan Impact edition. It typically contains a year, an application start
  * and end date (and maybe other stuff in the future if we can find out what)
  *
  * @author Louis Vialar
  */
object EditionsModel {

  class EditionsModel @Inject()(mongo: MongoDBService) {
    private def collection: MongoCollection[Document] = mongo.database.getCollection("editions")

    def getAllEditions: Future[Seq[EditionWrapper]] =
      collection.find.map(EditionWrapper(_)).toFuture

    def getEdition(year: String): Future[EditionWrapper] =
      collection.find(Filters.eq("year", year)).first.toFuture.map(EditionWrapper(_))

    def getActiveEditions: Future[Seq[EditionWrapper]] =
      getAllEditions.map(_.filter(_.isActive))

    def setEdition(editionWrapper: EditionWrapper): Future[UpdateResult] =
      collection.replaceOne(Filters.eq("year", editionWrapper.year), editionWrapper.toDocument, UpdateOptions().upsert(true)).toFuture
  }


  case class EditionWrapper(applicationsStart: Date, applicationsEnd: Date, year: String) {
    lazy val toDocument: Document = Document(
      "applicationsStart" -> applicationsStart,
      "applicationsEnd" -> applicationsEnd,
      "year" -> year
    )

    def withYear(y: String): EditionWrapper = EditionWrapper(applicationsStart, applicationsEnd, y)

    /**
      * Whether or not this edition accepts applications
      *
      * @return
      */
    def isActive: Boolean = {
      val today = new Date()
      (today before applicationsEnd) && (today after applicationsEnd)
    }
  }
  object EditionWrapper {
    def apply(document: Document): EditionWrapper = {
      EditionWrapper(document("applicationsStart"), document("applicationsEnd"), document("year"))
    }
  }

}
