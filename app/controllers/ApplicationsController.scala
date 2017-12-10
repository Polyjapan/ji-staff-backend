package controllers

import javax.inject._

import akka.actor.ActorSystem
import data.{Application, Edition}
import models.{ApplicationsModel, EditionsModel}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.AuthParserService
import tools.FutureMappers

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: ApplicationsModel, editionsModel: EditionsModel)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  /**
    * Endpoint /applications/:year/:userid/:page <br/>
    * Allows an admin to update someone else's application
    * @param year the edition for which the application is made
    * @param userid the user whose application is to be updated
    * @param page the page of the application to update
    */
  def adminUpdateApplication(year: String, userid: String, page: Int): Action[AnyContent] = Action.async { implicit request =>
    (auth.isAdmin, request.body.asJson) match {
      case ((false, _), _) => Future(Unauthorized)
      case ((_, _), None) => Future(BadRequest)
      case ((true, token), Some(json: JsObject)) => doUpdateApplication(year, userid, "", page, json, bypassValidated = true)
      case _ => Future(BadRequest)
    }
  }

  /**
    * Endpoint /applications/:year/:page <br>
    * Updates the authenticated user's application
    * @param year the edition for which the application is made
    * @param page the page of the application to update
    */
  def updateApplication(year: String, page: Int): Action[AnyContent] = Action.async { implicit request =>
    (auth.isOnline, request.body.asJson) match {
      case ((false, _), _) => Future(Unauthorized)
      case ((_, _), None) => Future(BadRequest)
      case ((true, token), Some(json: JsObject)) => doUpdateApplication(year, token.getSubject, token.getClaim("email").asString(), page, json)
      case _ => Future(BadRequest)
    }
  }

  /**
    * Update an application using the request content
    * @param year the edition for which the application is made
    * @param userid the id of the user applying (token subject)
    * @param email the email of the user applying (used to create the application if it doesn't exist yet)
    * @param page the page of the application to update (refers to a page in the edition)
    * @param data the data contained in the request body
    * @param bypassValidated if true, a validated application will still be modifiable
    * @return a future holding the result
    */
  private def doUpdateApplication(year: String, userid: String, email: String, page: Int, data: JsObject, bypassValidated: Boolean = false): Future[Result] = {    def update(application: Application, edition: Option[Edition]): Future[Result] = {
      if (edition.isEmpty) Future(NotFound(Json.obj("messages" -> List("Cette édition n'existe pas"))))
      else if (!edition.get.isActive) Future(BadRequest(Json.obj("messages" -> List("Les inscriptions sont fermées pour cette édition"))))
      else if (!edition.get.formData.exists(_.pageNumber == page)) Future(NotFound(Json.obj("messages" -> List("Cette page n'existe pas"))))
      else edition.get.formData.filter(_.pageNumber == page).head.verifyPageAndBuildObject(data, application.content) match {
        case (true, _, content) =>
          // We try to set the content that we know is valid
          application.withContent(content, bypassValidated) match {
            // We save it in the database or throw an error
            case (true, updatedApplication) =>
              model.setApplication(updatedApplication).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
            case (false, _) => Future(BadRequest(Json.obj("messages" -> List("Impossible de modifier une candidature envoyée"))))
          }
        case (false, err, _) => Future(BadRequest(Json.obj("messages" -> err)))
      }
    }

    val defaultApplication = Application(userid, email, year) // Build a default application in case it doesn't exist in the database
    for {
      // Get application and edition from database
      application <- model.getApplication(year, userid)
      edition <- editionsModel.getEdition(year)

      // Compute the result of the update
      result <- update(application.getOrElse(defaultApplication), edition)
    } yield result
  }

  /**
    * Endpoint /applications/:year (PUT) <br>
    * Mark an application as validated, meaning the user will not be able to modify it anymore<br>
    * It also should check that the application CAN be validated <=> all the required fields are present and valid
    * @param year the year for which the application is made
    */
  def validateApplication(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      def validate(application: Option[Application], edition: Option[Edition]): Future[Result] = {
        if (application.isEmpty) Future(BadRequest(Json.obj("messages" -> List("Aucune candidature à valider pour cette année"))))
        else if (edition.isEmpty) Future(NotFound(Json.obj("messages" -> List("Cette édition n'existe pas"))))
        else if (!edition.get.isActive) Future(BadRequest(Json.obj("messages" -> List("Les inscriptions sont fermées pour cette édition"))))
        else application.get.validate(edition.get) match {
          case (true, _, application) => model.setApplication(application).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
          case (false, err, _) => Future(BadRequest(Json.obj("messages" -> err)))
        }
      }

      auth.isOnline match {
        case (true, data) =>
          for {
            application <- model.getApplication(year, data.getSubject)
            edition <- editionsModel.getEdition(year)
            result <- validate(application, edition)
          } yield result
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year (GET)<br>
    * Enables an user to access their own application for a given edition
    * @param year the year for which the user wants to see their application
    */
  def getApplication(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isOnline match {
        case (true, data) => model.getApplication(year, data.getSubject) map (_.map(_.removeSensitiveFields)) map optionalMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year/validated <br/>
    * Enables an admin to list all the applications sent for a given edition
    * @param year the year for which the admin wants to see the applications
    */
  def getSentApplications(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isAdmin match {
        case (true, _) => model.getAllValidated(year) map listMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year/waiting <br/>
    * Enables an admin to list all the applications sent and not yet given a response for a given edition
    * @param year the year for which the admin wants to see the applications
    */
  def getWaitingApplications(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isAdmin match {
        case (true, _) => model.getAllWaiting(year) map listMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year/accepted <br/>
    * Enables an admin to list all the accepted applications for a given edition
    * @param year the year for which the admin wants to see the applications
    */
  def getAcceptedApplications(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isAdmin match {
        case (true, _) => model.getAllAccepted(year) map listMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year/:userid <br/>
    * Enables an admin to get an application sent by a given user for a given edition
    * @param year the year for which the admin wants to see the application
    */
  def getApplicationByUser(year: String, userid: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isAdmin match {
        case (true, _) => model.getApplication(year, userid) map optionalMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year/refused <br/>
    * Enables an admin to list all the refused applications for a given edition
    * @param year the year for which the admin wants to see the applications
    */
  def getRefusedApplications(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isAdmin match {
        case (true, _) => model.getAllRefused(year) map listMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year/accepted (PUT) <br/>
    * Enables an admin to accept an application. The request body must contain the id of the user for which the application
    * is being accepted. <br/>
    * The request is passed to the `setState` method handling the actual database update
    * @param year the year for which the admin wants to set the application state
    */
  def setAccepted(year: String): Action[AnyContent] = Action.async {
    implicit request => this.setState(year, true)
  }

  /**
    * Endpoint /applications/:year/refused (PUT) <br/>
    * Enables an admin to refuse an application. The request body must contain the id of the user for which the application
    * is being refused. <br/>
    * The request is passed to the `setState` method handling the actual database update
    * @param year the year for which the admin wants to set the application state
    */
  def setRefused(year: String): Action[AnyContent] = Action.async {
    implicit request => this.setState(year, false)
  }

  /**
    * Updates the state of an application
    * @param year the year of the application
    * @param accepted the status (true = accepted, false = refused)
    * @param request the request whose body contains the userId
    * @return the request result
    */
  private def setState(year: String, accepted: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    (auth.isAdmin, request.body.asJson) match {
      case ((true, token), Some(json: JsObject)) =>   // admin with valid json in the request body
        model.getApplication(year, json("userId").as[String]).flatMap {
          case Some(application) => // the body contains an userId field corresponding to an actual application
            // We update the application and insert it in the database
            model.setApplication(application.accept(token.getSubject, token.getClaim("name").asString(), accepted))
              .map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
          case None => Future(NotFound) // no userId field or no application
        }

      case ((false, _), _) => Future(Unauthorized) // not an admin
      case ((true, _), _) => Future(BadRequest)   // admin with invalid data
    }
  }
}
