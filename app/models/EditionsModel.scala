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

  /**
    * Get all the editions present in the database
    * @return a future providing a sequence of editions
    */
  def getAllEditions: Future[Seq[Edition]] =
    collection
      .flatMap(_.find(Json.obj())
        .cursor[Edition](ReadPreference.primary)
        .collect[List](-1, FailOnError[List[Edition]]()))

  /**
    * Get the edition for a given year
    * @param year the year of the edition to find
    * @return a future providing an optional edition, depending on whether or not an edition existed for this year
    */
  def getEdition(year: String): Future[Option[Edition]] =
    collection
      .flatMap(_.find(Json.obj("year" -> year))
        .one[Edition](ReadPreference.primary))

  /**
    * Get all the editions that are active. <br/>
    * See [[EditionsModel.getAllEditions]] and [[Edition.isActive]] for more information.
    * @return a future providing a sequence of editions
    */
  def getActiveEditions: Future[Seq[Edition]] =
    getAllEditions.map(_.filter(_.isActive))

  /**
    * Sets an edition in the database <br/>
    * If an entry with the same year exists in the database, this entry will be updated with the new data<br/>
    * If no entry exists with the same year, it will be inserted
    * @param editionWrapper the edition to insert in the database
    * @return a future providing the update result
    */
  def setEdition(editionWrapper: Edition): Future[UpdateWriteResult] =
    collection.flatMap(_.update(Json.obj("year" -> editionWrapper.year), editionWrapper, upsert = true))
}

