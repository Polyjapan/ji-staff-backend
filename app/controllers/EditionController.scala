package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.EditionsModel
import play.api.mvc._
import services.AuthParserService
import tools.{FutureMappers, TemporaryEdition}

import scala.concurrent.ExecutionContext

@Singleton
class EditionController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: EditionsModel)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  def getAll: Action[AnyContent] = Action.async {
    model.getAllEditions map listMapper
  }

  def getActive: Action[AnyContent] = Action.async {
    model.getActiveEditions map listMapper
  }


  def getEdition(year: String): Action[AnyContent] = Action.async {
    model.getEdition(year) map optionalMapper
  }

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
