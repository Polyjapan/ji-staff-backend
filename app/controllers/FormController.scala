package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.FormModel._
import play.api.libs.json.Json
import play.api.mvc._
import services.AuthParserService
import tools.{FutureMappers, TemporaryForms}

import scala.concurrent.ExecutionContext

@Singleton
class FormController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: FormModel)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  def getForm(formId: String): Action[AnyContent] = {
    Action.async {
      model getForm formId map optionalMapper
    }
  }

  /**
    * Sets the form passed in the body as the form with the given id<br>
    * The body of the request is expected to be the exact same format of a document we could pull from the database<br>
    * // TODO : write down somewhere the actual structure of these documents. For now you can find'em in the FormModel file
    *
    * @param formId the id of the form
    */
  def setForm(formId: String): Action[AnyContent] = Action {
    //TemporaryForms.createForms(model)
    Ok
  }

  /*{
     Action.async { implicit request => {
       if (auth.isAdmin._1) {
         val jsonData = Document(request.body.asText.get)
         val form = Form(jsonData) withId formId
         model setForm form transform {
           case Success(_) => Try.apply(Ok("OK"))
           case Failure(t) => Try.apply(InternalServerError(t.getMessage))
         }
       } else {
         Future {
           Unauthorized
         }(exec)
       }
     }
     }
   }*/
}
