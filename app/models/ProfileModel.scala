package models

import javax.inject.Inject

import play.api.libs.json.{Format, JsObject, Json, OFormat}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.play.json._

/**
  * The profile contains a lot of "random" values and a map of applications by year
  * <br>The random values are actually the values configured by the site administrator. This pattern allows more
  * flexibility: the site owner can add and remove profile fields / application fields without having us, developers,
  * to update the code. That's why Mongo is a great DB system.
  *
  * @author Louis Vialar
  */
object ProfileModel {

  class ProfileModel @Inject()(val reactiveMongoApi: ReactiveMongoApi, implicit val ec: ExecutionContext) {
    private def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("profiles"))

    def getProfile(userid: String): Future[Option[ProfileWrapper]] =
      collection.flatMap(_.find(Json.obj("userId" -> userid)).one[ProfileWrapper])

    /**
      * Returns all then profiles that have an application the given year.
      * The candidatures may not have been sent
      *
      * @param year the year to look for
      * @return a list of profiles having an application for the given year
      */
    def getApplications(year: String): Future[Seq[ProfileWrapper]] =
      collection.flatMap(_.find(
        Json.obj("applications.year" -> Json.obj("$exists" -> true))
      ).cursor[ProfileWrapper](ReadPreference.primary).collect[Seq](-1, FailOnError[Seq[ProfileWrapper]]()))

    /**
      * Returns all the profiles that have sent an application the given year.
      *
      * @param year the year to look for
      * @return a list of profiles having a sent application for the given year
      */
    def getSentApplications(year: String): Future[Seq[ProfileWrapper]] =
    // Future.map works directly on the sequence
    // We filter this sequence checking that the application was actually sent
    // In a future where we have thousands of unsent applications, we might want to filter during the query and not
    // while processinge the results
      getApplications(year).map(_.filter(_.application(year).wasSent))

    /**
      * Update (or insert) the provided profile. <br>
      * The update selector is the `_id` field of the document. If it's absent or if there is no document with this _id,
      * a new one will be created.
      *
      * @param profileWrapper The profile to update
      * @return A future describing the result of the update
      */
    def updateOrInsertProfile(profileWrapper: ProfileWrapper): Future[UpdateWriteResult] =
      collection.flatMap(_.update(Json.obj("userId" -> profileWrapper.userId), profileWrapper, upsert = true))


  }

  /**
    * A wrapper for profiles, providing some helper methods to access and modify applications
    *
    * @param profile the underlying document containing the profile
    */
  case class ProfileWrapper(userId: String, profile: JsObject, applications: Map[String, JsObject]) {
    def wrappedApplications: Map[String, ApplicationWrapper] = applications.mapValues(ApplicationWrapper)

    /**
      * Get the application for a given year
      *
      * @param year the year of the requested application
      * @return the application for that year (might be an empty document if no application were made)
      */
    def application(year: String): ApplicationWrapper = wrappedApplications(year)

    /**
      * Return a new ProfileWrapper containing an updated application for a given year
      *
      * @param year        the year that should be updated
      * @param application the application to set that year
      * @return an updated ProfileWrapper
      */
    def withApplication(year: String, application: ApplicationWrapper) =
      ProfileWrapper(userId, profile, applications + (year -> application.application))
  }

  object ProfileWrapper {
    implicit val profileFormat: OFormat[ProfileWrapper] = Json.format[ProfileWrapper]
  }

  /**
    * A wrapper for applications, providing some helpers to access the applications data
    *
    * @param application the underlying document containing the application
    */
  case class ApplicationWrapper(application: JsObject) {
    /**
      * Checks if the application was actually sent by the user or not
      *
      * @return true if the application was sent by the user, false if it was not
      */
    def wasSent: Boolean = application.keys("sent") && application("sent").as[Boolean]
  }

}
