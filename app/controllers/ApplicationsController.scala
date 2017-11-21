package controllers

import javax.inject._

import akka.actor.ActorSystem
import com.auth0.jwt.interfaces.DecodedJWT
import models.EditionsModel.{EditionWrapper, EditionsModel}
import models.FormModel
import models.FormModel.{DateDecorator, Form, FormEntry, FormModel}
import models.ProfileModel.{ProfileModel, ProfileWrapper}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.AuthParserService
import tools.FutureMappers

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: FormModel, profileModel: ProfileModel, editionsModel: EditionsModel)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
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
    def processProfile(optProfile: Option[ProfileWrapper], optForm: Option[Form], edition: Option[EditionWrapper]): Future[Result] = {
      if (edition.isEmpty) return Future(NotFound(Json.toJson("error" -> "Edition non trouvée")))
      else if (!edition.get.isActive) return Future(MethodNotAllowed(Json.toJson("error" -> "Cette édition n'accepte plus de candidats")))

      val profile = optProfile.getOrElse(ProfileWrapper(data.getSubject, null, Json.obj(), Map()))
        .withEmail(data.getClaim("email").asString)

      val applicationWrap = profile.application(year) // get the application

      if (applicationWrap.wasSent) Future(MethodNotAllowed(Json.toJson("error" -> "Impossible de modifier une candidature envoyée")))
      else optForm match {
        case Some(form) =>
          val (errors, data) = FormModel.mapFormWithInput(form, applicationWrap.application, json)
          if (errors.isEmpty)
            (profileModel updateOrInsertProfile profile.withApplication(year, data))
              .map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
          else Future(BadRequest(Json.toJson(Json.obj("errors" -> errors))))
        case None => Future(InternalServerError(Json.toJson(Json.obj("error" -> "Formulaire non trouvé"))))
      }
    }

    for {
      optProfile <- profileModel.getProfile(data.getSubject)
      form <- model.getForm("application")
      edition <- editionsModel.getEdition(year)
      result <- processProfile(optProfile, form, edition)
    } yield result
  }

  def sendApplication(year: String): Action[AnyContent] = Action.async { implicit request =>
    auth.isOnline match {
      case (true, data) =>
        for {
          optProfile <- profileModel.getProfile(data.getSubject)
          applicationForm <- model.getForm("application")
          profileForm <- model.getForm("profile")
          edition <- editionsModel.getEdition(year)
          check <- checkApplication(optProfile, applicationForm, profileForm, edition)
        } yield check
      case (false, _) => Future(Unauthorized)
    }
  }

  def checkApplication(optProfile: Option[ProfileWrapper], applicationForm: Option[Form], profileForm: Option[Form], edition: Option[EditionWrapper]): Future[Result] = {
    if (optProfile.isEmpty) Future(MethodNotAllowed(Json.toJson("error" -> "Profil non rempli")))
    else if (edition.isEmpty) Future(NotFound(Json.toJson("error" -> "Edition non trouvée")))
    else if (!edition.get.isActive) Future(MethodNotAllowed(Json.toJson("error" -> "Cette édition n'accepte plus de candidats")))
    else {
      val profile = optProfile.get.profile
      val application = optProfile.get.application(edition.get.year)

      // Check : mineur au démarrage de JI ?
      val birthDateString: Option[String] = profile("birthdate").asOpt[String]
      val minor: Boolean = birthDateString.map(DateDecorator.extractDate(_, yearOffset = -18)).exists(_ before edition.get.conventionStart)

      // Check : fields obligatoires
      val missingApplicationFields = FormModel.listMissingFields(applicationForm.get, application.application, minor)
      val missingProfileFields = FormModel.listMissingFields(profileForm.get, profile, minor)

      if (missingApplicationFields.nonEmpty || missingProfileFields.nonEmpty) {
        val profileMessage = "Profil incomplet : " + missingApplicationFields.map(_._2).mkString(", ")
        val applicationMessage = "Candidature incomplet : " + missingProfileFields.map(_._2).mkString(", ")

        val message = (missingApplicationFields.nonEmpty, missingProfileFields.nonEmpty) match {
          case (true, true) => profileMessage + "\n" + applicationMessage
          case (true, false) => applicationMessage
          case (false, true) => profileMessage
        }

        return Future(MethodNotAllowed(Json.toJson("error" -> message)))
      }

      // ça a l'air bon, on met à jour le profil et on envoie
      (profileModel updateOrInsertProfile
        optProfile.get.withApplication(edition.get.year, application.withSent(true).application)
        ).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
    }
  }

  def getSentApplications(year: String) = TODO

  def setAccepted(userId: String, year: String) = TODO
}
