package models

import javax.inject.Inject

import org.mongodb.scala.model.{Filters, UpdateOptions}
import org.mongodb.scala.{Document, MongoCollection}
import services.MongoDBService

import scala.concurrent.Future
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.UpdateResult

/**
  * The profile contains a lot of "random" values and a map of applications by year
  * <br>The random values are actually the values configured by the site administrator. This pattern allows more
  * flexibility: the site owner can add and remove profile fields / application fields without having us, developers,
  * to update the code. That's why Mongo is a great DB system.
  *
  * @author Louis Vialar
  */
class ProfileModel @Inject()(mongo: MongoDBService) {
  private def collection: MongoCollection[Document] = mongo.database.getCollection("profiles")

  def getProfile(userid: String): Future[ProfileWrapper] =
    collection.find(Filters.eq("userId", userid)).first.toFuture.map(ProfileWrapper)

  /**
    * Returns all then profiles that have an application the given year.
    * The candidatures may not have been sent
    * @param year the year to look for
    * @return a list of profiles having an application for the given year
    */
  def getApplications(year: String): Future[Seq[ProfileWrapper]] =
    collection.aggregate(
      List(filter(exists(s"applications.$year")))
    ).map(ProfileWrapper).toFuture

  /**
    * Returns all the profiles that have sent an application the given year.
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
    * @param profileWrapper The profile to update
    * @return A future describing the result of the update
    */
  def updateOrInsertProfile(profileWrapper: ProfileWrapper): Future[UpdateResult] =
    updateOrInsertProfile(profileWrapper.profile)

  /**
    * Update (or insert) the provided profile. <br>
    * The update selector is the `_id` field of the document. If it's absent or if there is no document with this _id,
    * a new one will be created.
    * @param profile The profile to update
    * @return A future describing the result of the update
    */
  def updateOrInsertProfile(profile: Document): Future[UpdateResult] =
    collection.replaceOne(Filters.eq("_id", profile.getObjectId("_id")), profile, UpdateOptions().upsert(true)).toFuture


  /**
    * A wrapper for profiles, providing some helper methods to access and modify applications
    * @param profile the underlying document containing the profile
    */
  case class ProfileWrapper(profile: Document) {
    private def rawApplications: Map[String, Document] = profile.get[Map[String, Document]]("applications").getOrElse(Map()).withDefaultValue(Document());

    def applications: Map[String, ApplicationWrapper] = rawApplications.mapValues(ApplicationWrapper)

    /**
      * Get the application for a given year
      * @param year the year of the requested application
      * @return the application for that year (might be an empty document if no application were made)
      */
    def application(year: String): ApplicationWrapper = applications(year)

    /**
      * Return a new ProfileWrapper containing an updated application for a given year
      * @param year the year that should be updated
      * @param application the application to set that year
      * @return an updated ProfileWrapper
      */
    def withApplication(year: String, application: ApplicationWrapper) =
      ProfileWrapper(
        // we set the "applications" key of the profile
        // we set it to the existing applications updated with the new application for the selected year
        profile + ("applications" -> (rawApplications + (year -> application.application)).toSeq)
      )

  }

  /**
    * A wrapper for applications, providing some helpers to access the applications data
    * @param application the underlying document containing the application
    */
  case class ApplicationWrapper(application: Document) {
    /**
      * Checks if the application was actually sent by the user or not
      * @return true if the application was sent by the user, false if it was not
      */
    def wasSent: Boolean = application.get[Boolean]("sent") getOrElse false // by default it was not validated
  }
}
