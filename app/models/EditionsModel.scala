package models

import java.util.Date
import javax.inject.Inject

import play.api.libs.json.{Format, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.play.json._

/**
  * An edition is a set of parameters about a Japan Impact edition. It typically contains a year, an application start
  * and end date (and maybe other stuff in the future if we can find out what)
  *
  * @author Louis Vialar
  */
object EditionsModel {

  class EditionsModel @Inject()(val reactiveMongoApi: ReactiveMongoApi, implicit val ec: ExecutionContext) {
    private def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("editions"))

    def getAllEditions: Future[Seq[EditionWrapper]] = {
      collection.flatMap(_.find(Json.obj()).cursor[EditionWrapper](ReadPreference.primary).collect[List](-1, FailOnError[List[EditionWrapper]]()))

    }

    def getEdition(year: String): Future[Option[EditionWrapper]] =
      collection.flatMap(_.find(Json.obj("year" -> year)).one[EditionWrapper](ReadPreference.primary))

    def getActiveEditions: Future[Seq[EditionWrapper]] =
      getAllEditions.map(_.filter(_.isActive))

    def setEdition(editionWrapper: EditionWrapper): Future[UpdateWriteResult] =
      collection.flatMap(_.update(Json.obj("year" -> editionWrapper.year), editionWrapper, upsert = true))
  }


  case class EditionWrapper(applicationsStart: Date, applicationsEnd: Date, year: String) {
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
    implicit val editionFormat: OFormat[EditionWrapper] = Json.format[EditionWrapper]
  }


}
