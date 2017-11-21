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
class ProfileController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, formModel: FormModel, profileModel: ProfileModel)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  def getProfile: Action[AnyContent] = Action.async { implicit request =>
    auth.isOnline match {
      case (true, data) => profileModel.getProfile(data.getSubject) map optionalMapper
      case (false, _) => Future(Unauthorized)
    }
  }

  def setProfile: Action[AnyContent] = Action.async { implicit request =>
    (auth.isOnline, request.body.asJson) match {
      case ((true, data), Some(json: JsObject)) => doSetProfile(data, json)
      case ((false, _), _) => Future(Unauthorized)
      case ((_, _), None) => Future(BadRequest)
      case ((_, _), Some(_)) => Future(BadRequest)
    }
  }

  private def doSetProfile(data: DecodedJWT, json: JsObject): Future[Result] = {
    def processProfile(optProfile: Option[ProfileWrapper], optForm: Option[Form]): Future[Result] = {
      val profile = optProfile.getOrElse(ProfileWrapper(data.getSubject, null, Json.obj(), Map()))
        .withEmail(data.getClaim("email").asString)

      optForm match {
        case Some(form) =>
          val (errors, data) = FormModel.mapFormWithInput(form, profile.profile, json)
          if (errors.isEmpty)
            (profileModel updateOrInsertProfile profile.withProfile(data))
            .map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
          else Future(BadRequest(Json.toJson(Json.obj("errors" -> errors))))
        case None => Future(InternalServerError(Json.toJson(Json.obj("error" -> "form not found"))))
      }
    }

    for {
      optProfile <- profileModel.getProfile(data.getSubject)
      form <- formModel.getForm("profile")
      result <- processProfile(optProfile, form)
    } yield result
  }
}