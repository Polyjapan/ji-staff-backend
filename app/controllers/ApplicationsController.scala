package controllers

import javax.inject._

import akka.actor.ActorSystem
import data.{Application, Comment, Edition}
import models.{ApplicationsModel, EditionsModel}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, _}
import services.{Auth0ManagementService, AuthParserService, UploadsService}
import tools.FutureMappers

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: ApplicationsModel, editionsModel: EditionsModel, uploads: UploadsService, auth0: Auth0ManagementService)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  /**
    * Endpoint /applications/:year/:userid/:page <br/>
    * Allows an admin to update someone else's application
    *
    * @param year   the edition for which the application is made
    * @param userid the user whose application is to be updated
    * @param page   the page of the application to update
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
    * Creates an already accepted application with no content. This generates an unique ID for this application to
    * allow an user to claim it.
    *
    * @param year the year for the application
    */
  def adminCreateApplication(year: String): Action[AnyContent] = Action.async { implicit request =>
    auth.isAdmin match {
      case (false, _) => Future(Unauthorized)
      case (true, token) =>
        val app = Application.unclaimed(year).setValidated.accept(token.getSubject, token.getClaim("name").asString(), accepted = true)
        model.setApplication(app).map(_ => Ok(Json.toJson(app)))
      case _ => Future(BadRequest)
    }
  }

  /**
    * Allows an user to become the owner of an admin created application
    *
    * @param year       the year for which the user wishes to claim an application
    * @param claimToken the token of the application to claim
    * @return a [[BadRequest]] if the claim didn't succeed, a [[Ok]] containing the serialized new [[Application]] if it
    *         did
    */
  def userClaimApplication(year: String, claimToken: String): Action[AnyContent] = Action.async { implicit request =>
    auth.isOnline match {
      case (false, _) => Future(Unauthorized)
      case (true, token) =>
        model.getApplication(year, claimToken)
          .map(_ flatMap (_.claimApplication(token.getSubject, token.getClaim("email").asString())))
          .flatMap {
            case Some(app) =>
              auth0.makeStaff(app.userId, year)
              model.removeApplication(claimToken, year)
              model.setApplication(app).map(_ => Ok(Json.toJson(app.removeSensitiveFields)))
            case None =>
              Future(BadRequest(Json.obj("messages" -> List("Le jeton de candidature n'est pas utilisable."))))
          }
    }
  }

  /**
    * Endpoint /applications/:year/:page <br>
    * Updates the authenticated user's application
    *
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

  def updateParentalAuthorization(year: String): Action[TemporaryFile] =
    uploadAndCall(year, UploadsService.pdf :: UploadsService.images,
      (app, path) => app.updateParentalAuthorization(path), !_.isParentalAllowanceAccepted)

  def updatePicture(year: String): Action[TemporaryFile] =
    uploadAndCall(year, UploadsService.images, (app, path) => app.updatePicture(path))

  def adminUpdatePicture(year: String, userId: String): Action[TemporaryFile] = Action.async(parse.temporaryFile) { implicit request =>
    auth.isAdmin match {
      case (false, _) => Future(Unauthorized(Json.obj("messages" -> List("Vous n'êtes pas admin"))))
      case (true, token) =>
        for {
          // Get application and edition from database
          application <- model.getApplication(year, userId)
          edition <- editionsModel.getEdition(year)

          res <- {
            if (edition.isEmpty) Future(NotFound(Json.obj("messages" -> List("Cette édition n'existe pas"))))
            else if (application.isEmpty) Future(NotFound(Json.obj("messages" -> List("Vous n'avez pas envoyé de candidature pour cette édition"))))
            else uploads.upload(request.body, UploadsService.images) match {
              case (false, _) => Future(BadRequest(Json.obj("messages" -> List("Le format de fichier est incorrect"))))
              case (true, path) =>
                model.setApplication(application.get.updatePicture(path)).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
            }
          }
        } yield res
    }
  }

  private def uploadAndCall(year: String, allowedTypes: List[UploadsService.MimeType],
                            applyNext: (Application, String) => Application,
                            allowUpdate: Application => Boolean = _ => true): Action[TemporaryFile] = Action.async(parse.temporaryFile) { implicit request =>
    auth.isStaff(year) match {
      case (false, _) => Future(Unauthorized(Json.obj("messages" -> List("Vous n'êtes pas staff sur cette édition"))))
      case (true, token) =>
        for {
          // Get application and edition from database
          application <- model.getApplication(year, token.getSubject)
          edition <- editionsModel.getEdition(year)

          res <- {
            if (edition.isEmpty) Future(NotFound(Json.obj("messages" -> List("Cette édition n'existe pas"))))
            else if (application.isEmpty) Future(NotFound(Json.obj("messages" -> List("Vous n'avez pas envoyé de candidature pour cette édition"))))
            else if (!allowUpdate(application.get)) Future(BadRequest(Json.obj("messages" -> List("Impossible de mettre à jour ce fichier car il est en lecture seule"))))
            else uploads.upload(request.body, allowedTypes) match {
              case (false, _) => Future(BadRequest(Json.obj("messages" -> List("Le format de fichier est incorrect"))))
              case (true, path) =>
                model.setApplication(applyNext(application.get, path)).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
            }
          }
        } yield res
    }
  }

  /**
    * Update an application using the request content
    *
    * @param year            the edition for which the application is made
    * @param userid          the id of the user applying (token subject)
    * @param email           the email of the user applying (used to create the application if it doesn't exist yet)
    * @param page            the page of the application to update (refers to a page in the edition)
    * @param data            the data contained in the request body
    * @param bypassValidated if true, a validated application will still be modifiable
    * @return a future holding the result
    */
  private def doUpdateApplication(year: String, userid: String, email: String, page: Int, data: JsObject, bypassValidated: Boolean = false): Future[Result] = {
    def update(application: Application, edition: Option[Edition]): Future[Result] = {
      if (edition.isEmpty) Future(NotFound(Json.obj("messages" -> List("Cette édition n'existe pas"))))
      else if (!edition.get.isActive && !bypassValidated) Future(BadRequest(Json.obj("messages" -> List("Les inscriptions sont fermées pour cette édition"))))
      else if (!edition.get.formData.exists(_.pageNumber == page)) Future(NotFound(Json.obj("messages" -> List("Cette page n'existe pas"))))
      else edition.get.formData.filter(_.pageNumber == page).head.verifyPageAndBuildObject(data, application.content) match {
        case (true, _, content) =>
          // We try to set the content that we know is valid
          application.withContent(content, bypassValidated) match {
            // We save it in the database or throw an error
            case Some(updatedApplication) =>
              model.setApplication(updatedApplication).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
            case None => Future(BadRequest(Json.obj("messages" -> List("Impossible de modifier une candidature envoyée"))))
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
    *
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
    *
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
    *
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
    *
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
    *
    * @param year the year for which the admin wants to see the applications
    */
  def getAcceptedApplications(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      auth.isAdmin match {
        case (true, data) => model.getAllAccepted(year) map listMapper
        case (false, _) => Future(Unauthorized)
      }
  }

  /**
    * Endpoint /applications/:year/:userid <br/>
    * Enables an admin to get an application sent by a given user for a given edition
    *
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
    *
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
    *
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
    *
    * @param year the year for which the admin wants to set the application state
    */
  def setRefused(year: String): Action[AnyContent] = Action.async {
    implicit request => this.setState(year, false)
  }

  /**
    * Grant all applications their rights
    *
    * @param year the year of the applications
    */
  def refreshRights(year: String): Action[AnyContent] = Action.async { implicit request =>
    auth.isAdmin match {
      case (true, token) =>
        (model.getAllAccepted(year) map (_ map (app => {
          auth0.makeStaff(app.userId, year) // make the users staffs
          app.userId // rerturn their user id
        })))
          .flatMap(accepted => model.getAllRefused(year)
            map (refused => // map the Future[Seq] to Future[(Seq, Seq)] where the 1st seq is the list of granted ids and the second the list of ungranted ids
            (accepted, refused map (app => {
              auth0.unmakeStaff(app.userId, year) // unmake the refused staffs
              app.userId // return their user id
            }))))
          .flatMap(pair => model.getAllWaiting(year)
            map (waiting => (pair._1, pair._2 ++ (waiting map (app => {
            auth0.unmakeStaff(app.userId, year)
            app.userId
          })))))
          .map(u => Ok(Json.toJson(Json.obj("granted" -> u._1, "removed" -> u._2))))


      case _ => Future(Unauthorized) // not an admin
    }
  }

  /**
    * Updates the state of an application
    *
    * @param year     the year of the application
    * @param accepted the status (true = accepted, false = refused)
    * @param request  the request whose body contains the userId
    * @return the request result
    */
  private def setState(year: String, accepted: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    (auth.isAdmin, request.body.asJson) match {
      case ((true, token), Some(json: JsObject)) => // admin with valid json in the request body
        model.getApplication(year, json("userId").as[String]).flatMap {
          case Some(application) => // the body contains an userId field corresponding to an actual application
            // We add or remove the staff
            if (accepted) auth0.makeStaff(application.userId, year)
            else auth0.unmakeStaff(application.userId, year)

            // We update the application and insert it in the database
            model.setApplication(application.accept(token.getSubject, token.getClaim("name").asString(), accepted))
              .map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
          case None => Future(NotFound) // no userId field or no application
        }

      case ((false, _), _) => Future(Unauthorized) // not an admin
      case ((true, _), _) => Future(BadRequest) // admin with invalid data
    }
  }

  /**
    * Accepts or refuses a parental authorization for a user
    *
    * @param year the year for the application
    */
  def adminAcceptParentalAllowance(year: String): Action[AnyContent] = Action.async { implicit request =>
    (auth.isAdmin, request.body.asJson) match {
      case ((false, _), _) => Future(Unauthorized)
      case ((true, token), Some(json: JsObject)) =>
        if (!json.keys("userId") || !json.keys("status")) Future(BadRequest)
        else model.getApplication(year, json("userId").as[String]).flatMap {
          case Some(application) => // the body contains an userId field corresponding to an actual application
            // We update the application and insert it in the database
            model.setApplication(
              if (json("status").as[Boolean]) // if it is accepted
                application.acceptParentalAuthorization
              else // else we read the refuse reason
                application.refuseParentalAuthorization(json.value.get("reason").flatMap(_.asOpt[String]).getOrElse(""))
            ).map(result => Ok(Json.toJson(Json.obj("n" -> result.n))))
          case None => Future(NotFound) // no userId field or no application
        }
      case _ => Future(BadRequest)
    }
  }


  /**
    * Endpoint POST /applications/:year/comments <br/>
    * Enables admins to add comments to a given application <br/>
    * The request body is expected to contain fields "userId" and "comment"
    *
    * @param year the edition for the application
    */
  def addComment(year: String): Action[AnyContent] = Action.async {
    implicit request =>
      (auth.isAdmin, request.body.asJson) match {
        case ((false, _), _) => Future(Unauthorized)
        case ((_, _), None) => Future(BadRequest)
        case ((true, token), Some(json: JsObject)) =>
          val comment = Comment(token.getClaim("name").asString(), token.getSubject, System.currentTimeMillis, json("comment").as[String])

          model.getApplication(year, json("userId").as[String]).flatMap {
            case Some(application) =>
              model.setApplication(application withComment comment)
                .map(result => Ok(Json.toJson(Json.obj("n" -> result.n, "comment" -> comment))))
          }

        case _ => Future(BadRequest)
      }
  }

}
