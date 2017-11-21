package models

import javax.inject.Inject

import data.Edition
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class EditionsModel @Inject()(val reactiveMongoApi: ReactiveMongoApi, implicit val ec: ExecutionContext) {
  private def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("editions"))

  def getAllEditions: Future[Seq[Edition]] =
    collection
      .flatMap(_.find(Json.obj())
        .cursor[Edition](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Edition]]()))


  def getEdition(year: String): Future[Option[Edition]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year))
        .one[Edition](ReadPreference.primary))

  def getActiveEditions: Future[Seq[Edition]] =
    getAllEditions.map(_.filter(_.isActive))

  def setEdition(editionWrapper: Edition): Future[UpdateWriteResult] =
    collection.flatMap(_.update(Json.obj("year" -> editionWrapper.year), editionWrapper, upsert = true))
}

