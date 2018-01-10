package controllers

import javax.inject._

import akka.actor.ActorSystem
import data.Application
import models.EditionsModel
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc._
import services.{AuthParserService, UploadsService}
import tools.{FutureMappers, TemporaryEdition}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EditionController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: EditionsModel, uploads: UploadsService)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  /**
    * Endpoint /editions (GET) <br/>
    * Returns a list of all editions existing in the database
    */
  def getAll: Action[AnyContent] = Action.async {
    model.getAllEditions map listMapper
  }

  /**
    * Endpoint /editions/active (GET) <br/>
    * Returns a list of all active editions in the database, namely a list of editions accepting applications <br/>
    * This list will usually contain 0 or 1 element
    */
  def getActive: Action[AnyContent] = Action.async {
    model.getActiveEditions map listMapper
  }

  /**
    * Endpoint /editions/:year (GET) <br/>
    * Returns the details of an edition or a 404 error if it doesn't exist
    * @param year the year (id of the edition) to search
    */
  def getEdition(year: String): Action[AnyContent] = Action.async {
    model.getEdition(year) map optionalMapper
  }

  def uploadForm(year: String): Action[TemporaryFile] = Action(parse.temporaryFile) { implicit request =>
    auth.isOnline match {
      case (false, _) => Unauthorized
      case (true, token) =>
        uploads.upload(request.body, List(UploadsService.pdf), "formulaire-autorisation-" + year) match {
          case (true, _) => Ok
          case (false, _) => BadRequest
        }
    }
  }

  /**
    * Endpoint /editions/:year (PUT) <br/>
    * Updates the details of an edition. Not implemented yet, it just returns Ok for now.
    * @param year the id of the edition to update or create
    */
  def setEdition(year: String): Action[AnyContent] = Action {
    // TemporaryEdition.createEditions(model)
    Ok
  }
  /*Action.async { implicit request => {
       if (auth.isAdmin._1)
         model setEdition EditionWrapper(Document(request.body.asText.get)) transform {
           case Success(_) => Try.apply(Ok("OK"))
           case Failure(t) => Try.apply(InternalServerError(t.getMessage))
         }
       else Future {Unauthorized}
     }
     }*/
}
