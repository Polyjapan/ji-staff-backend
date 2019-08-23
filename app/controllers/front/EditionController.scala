package controllers.front

import javax.inject.{Inject, Singleton}
import models.EditionsModel
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class EditionController @Inject()(cc: ControllerComponents, model: EditionsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getEdition: Action[AnyContent] = Action async model.getCurrentEdition.map {
    case Some(res) => Ok(Json.toJson(res))
    case _ => NotFound
  }

}
