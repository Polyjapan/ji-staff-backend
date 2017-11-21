package controllers

import javax.inject._

import akka.actor.ActorSystem
import com.auth0.jwt.interfaces.DecodedJWT
import models.FormModel
import models.FormModel.{Form, FormModel}
import models.ProfileModel.{ProfileModel, ProfileWrapper}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.AuthParserService
import tools.FutureMappers

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: FormModel, profileModel: ProfileModel)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  /*
  # update your application for an edition
  PUT     /applications/:year         controllers.ApplicationsController.updateApplication(year: String)
  # get your application for an edition
  GET     /applications/:year         controllers.ApplicationsController.getApplication(year: String)
  # get all sent applications with user profile for an edition (comitee only)
  GET     /applications/sent/:year    controllers.ApplicationsController.getSentApplications(year: String)
  # accept or refuse an application (comitee only)
  PUT     /applications/:userId/:year controllers.ApplicationsController.setAccepted(userId: String, year: String)
   */
  def getApplication(year: String): Action[AnyContent] = Action.async { implicit request =>
    auth.isOnline match {
      case (true, data) => profileModel.getProfile(data.getSubject) map (_.map(_.applications(year))) map optionalMapper
      case (false, _) => Future(Unauthorized)
    }
  }

  def updateApplication(year: String): Action[AnyContent] = Action.async { implicit request =>
    (auth.isOnline, request.body.asJson) match {
      case ((true, data), Some(json: JsObject)) => doSetApplication(data, json, year)
      case ((false, _), _) => Future(Unauthorized)
      case ((_, _), None) => Future(BadRequest)
      case ((_, _), Some(_)) => Future(BadRequest)
    }
  }

  private def doSetApplication(data: DecodedJWT, json: JsObject, year: String): Future[Result] = {
    def processProfile(optProfile: Option[ProfileWrapper], optForm: Option[Form]): Future[Result] = {
      val profile = optProfile.getOrElse(ProfileWrapper(data.getSubject, null, Json.obj(), Map()))
        .withEmail(data.getClaim("email").asString)

      val application = profile.applications.withDefaultValue(Json.obj())(year) // get the application

      optForm match {
        case Some(form) =>
          val (errors, data) = FormModel.mapFormWithInput(form, application, json)
          if (errors.isEmpty)
            (profileModel updateOrInsertProfile profile.withApplication(year, data))
              .map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
          else Future(BadRequest(Json.toJson(Json.obj("errors" -> errors))))
        case None => Future(InternalServerError(Json.toJson(Json.obj("error" -> "form not found"))))
      }
    }

    for {
      optProfile <- profileModel.getProfile(data.getSubject)
      form <- model.getForm("application")
      result <- processProfile(optProfile, form)
    } yield result
  }

  def getSentApplications(year: String) = TODO

  def setAccepted(userId: String, year: String) = TODO
}
