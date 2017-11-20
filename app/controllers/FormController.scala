package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.FormModel._
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.libs.json._
import play.api.mvc._
import services.AuthParserService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class FormController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: FormModel)(implicit exec: ExecutionContext) extends AbstractController(cc) {
  def getForm(formId: String): Action[AnyContent] = {
    Action.async { implicit request => {
      model getForm formId transform {
          case Success(null) => Try.apply(NotFound())
          case Success(v) => Try.apply(Ok(Json.toJson(v)))
          case Failure(t) => Try.apply(InternalServerError(t.getMessage))
      }
    }
    }
  }

  /**
    * Sets the form passed in the body as the form with the given id<br>
    *   The body of the request is expected to be the exact same format of a document we could pull from the database<br>
    *     // TODO : write down somewhere the actual structure of these documents. For now you can find'em in the FormModel file
    * @param formId the id of the form
    */
  def setForm(formId: String): Action[AnyContent] = {
    Action.async { implicit request => {
      if (auth.isAdmin._1) {
        val jsonData = Document(request.body.asText.get)
        val form = Form(jsonData) withId formId
        model setForm form transform {
          case Success(_) => Try.apply(Ok("OK"))
          case Failure(t) => Try.apply(InternalServerError(t.getMessage))
        }
      } else {
        Future { Unauthorized() }(exec)
      }
    }
    }
  }
}
