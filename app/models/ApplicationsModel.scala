package models

import javax.inject.Inject

import data.{Application, Edition}
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
class ApplicationsModel @Inject()(val reactiveMongoApi: ReactiveMongoApi, implicit val ec: ExecutionContext) {
  private def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("applications"))

  def getAll(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  def getAllValidated(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year, "isValidated" -> true))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  def getAllWaiting(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year, "isValidated" -> true, "isAccepted" -> false, "isRefused" -> false))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  def getAllAccepted(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year, "isAccepted" -> true))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  def getAllRefused(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year, "isRefused" -> true))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  def getApplication(year: String, userId: String): Future[Option[Application]] =
    collection
    .flatMap(_.find(Json.obj("year" -> year, "userId" -> userId))
        .one[Application](ReadPreference.primary))

  def getApplication(objectId: String): Future[Option[Application]] =
    collection
    .flatMap(_.find(Json.obj("_id" -> objectId))
        .one[Application](ReadPreference.primary))

  def setApplication(application: Application): Future[UpdateWriteResult] =
    collection.flatMap(_.update(Json.obj("userId" -> application.userId, "year" -> application.year), application, upsert = true))
}

