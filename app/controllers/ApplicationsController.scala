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
  /*
  PUT     /applications/:year/:page   controllers.ApplicationsController.updateApplication(year: String, page: Int)
PUT     /applications/:year         controllers.ApplicationsController.validateApplication(year: String)
GET     /applications/:year         controllers.ApplicationsController.getApplication(year: String)

# get all sent applications with user profile for an edition (comitee only)
GET     /applications/sent/:year    controllers.ApplicationsController.getSentApplications(year: String)
# accept or refuse an application (comitee only)
PUT     /applications/:userId/:year controllers.ApplicationsController.setAccepted(userId: String, year: String)

   */
  def updateApplication(year: String, page: Int): Action[AnyContent] = Action.async { implicit request =>
    def update(application: Application, edition: Option[Edition],
               page: Int, data: JsObject): Future[Result] = {
      if (edition.isEmpty) Future(NotFound(Json.obj("messages" -> List("Cette édition n'existe pas"))))
      else if (!edition.get.isActive) Future(BadRequest(Json.obj("messages" -> List("Les inscriptions sont fermées pour cette édition"))))
      else if (!edition.get.formData.exists(_.pageNumber == page)) Future(NotFound(Json.obj("messages" -> List("Cette page n'existe pas"))))
      else edition.get.formData.filter(_.pageNumber == page).head.verifyPageAndBuildObject(data, application.content) match {
        case (true, _, content) =>
          // We try to set the content that we know is valid
          application.withContent(content) match {
            // We save it in the database or throw an error
            case (true, application) => model.setApplication(application).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
            case (false, _) => Future(BadRequest(Json.obj("messages" -> List("Impossible de modifier une candidature envoyée"))))
          }
        case (false, err, _) => Future(BadRequest(Json.obj("messages" -> err)))
      }
    }
    
    (auth.isOnline, request.body.asJson) match {
      case ((false, _), _) => Future(Unauthorized)
      case ((_, _), None) => Future(BadRequest)
      case ((true, token), Some(json: JsObject)) =>
        val defaultApplication = Application(token.getSubject, token.getClaim("email").asString(), year)
        for {
          application <- model.getApplication(year, token.getSubject)
          edition <- editionsModel.getEdition(year)
          result <- update(application.getOrElse(defaultApplication), edition, page, json)
        } yield result
      case _ => Future(BadRequest)
    }
  }

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

  def getApplication(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isOnline match {
        case (true, data) => model.getApplication(year, data.getSubject) map optionalMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  def getSentApplications(year: String): Action[AnyContent] = TODO

  def setAccepted(userId: String, year: String): Action[AnyContent] = TODO
}
