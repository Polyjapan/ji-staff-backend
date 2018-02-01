package models

import javax.inject.Inject

import data.{Application, Edition}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class ApplicationsModel @Inject()(val reactiveMongoApi: ReactiveMongoApi, implicit val ec: ExecutionContext) {

  private def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("applications"))

  /**
    * Get all the applications for a given year
    * @param year the year to search
    * @return a future providing a sequence of applications
    */
  def getAll(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  /**
    * Get all the validated applications for a given year <br/>
    * An application is validated if the applicant validated it, meaning it can't be updated anymore and all the required
    * fields are present and valid.
    * @param year the year to search
    * @return a future providing a sequence of applications
    */
  def getAllValidated(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year, "isValidated" -> true))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  /**
    * Get all the validated applications that didn't receive an answer for a given year
    * @param year the year to search
    * @return a future providing a sequence of applications
    */
  def getAllWaiting(year: String): Future[Seq[Application]] =
    getAllValidated(year).map(_.filter(app => !app.isRefused.getOrElse(false) && !app.isAccepted))

  /**
    * Get all the accepted applications for a given year
    * @param year the year to search
    * @return a future providing a sequence of applications
    */
  def getAllAccepted(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year, "isAccepted" -> true))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  /**
    * Get all the refused applications for a given year
    * @param year the year to search
    * @return a future providing a sequence of applications
    */
  def getAllRefused(year: String): Future[Seq[Application]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year, "isRefused" -> true))
        .cursor[Application](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Application]]()))

  /**
    * Get the application sent by a given user for a given year
    * @param year the year to search
    * @param userId the user to search
    * @return a future providing an optional application (depending on whether or not the application exists)
    */
  def getApplication(year: String, userId: String): Future[Option[Application]] =
    collection
    .flatMap(_.find(Json.obj("year" -> year, "userId" -> userId))
        .one[Application](ReadPreference.primary))

  /**
    * Get an application by its database id
    * @param objectId the id of the application in the database
    * @return a future providing an optional application, depending on whether or not the application exists
    */
  def getApplication(objectId: String): Future[Option[Application]] =
    collection
    .flatMap(_.find(Json.obj("_id" -> objectId))
        .one[Application](ReadPreference.primary))

  /**
    * Sets an application in the database <br/>
    * If an entry with the same userId and year exists in the database, this entry will be updated with the new data<br/>
    * If no entry exists with these two exact fields, it will be inserted
    * @param application the application to insert in the database
    * @return a future providing the update result
    */
  def setApplication(application: Application): Future[UpdateWriteResult] =
    collection.flatMap(_.update(Json.obj("userId" -> application.userId, "year" -> application.year), application, upsert = true))

  /**
    * Removes an application by its userId and year
    * @param userId the userId of the application to remove
    * @param year the year of the application to remove
    * @return
    */
  def removeApplication(userId: String, year: String): Future[WriteResult] =
    collection.flatMap(_.remove(Json.obj("userId" -> userId, "year" -> year)))

}

