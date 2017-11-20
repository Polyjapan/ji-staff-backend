package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.EditionsModel.{EditionWrapper, EditionsModel}
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.mvc._
import services.AuthParserService
import tools.Tools._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class EditionController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: EditionsModel)(implicit exec: ExecutionContext) extends AbstractController(cc) {
  def getAll: Action[AnyContent] = Action.async {
    model.getAllEditions.transform(jsonTransformer)
  }

  def getActive: Action[AnyContent] = Action.async {
    model.getActiveEditions.transform(jsonTransformer)
  }

  def setEdition(year: String): Action[AnyContent] = Action.async { implicit request => {
    if (auth.isAdmin._1)
      model setEdition EditionWrapper(Document(request.body.asText.get)) transform {
        case Success(_) => Try.apply(Ok("OK"))
        case Failure(t) => Try.apply(InternalServerError(t.getMessage))
      }
    else Future {
      Unauthorized()
    }(exec)
  }
  }

  def getEdition(year: String): Action[AnyContent] = Action.async {
    model.getEdition(year).transform(jsonTransformer)
  }
}
