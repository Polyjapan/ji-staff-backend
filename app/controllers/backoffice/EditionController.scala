package controllers.backoffice

import javax.inject.{Inject, Singleton}
import models.{EventsModel, FormsModel}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import utils.AuthenticationPostfix._
import play.api.Configuration

/**
 * @author Louis Vialar
 */
@Singleton
class EditionController @Inject()(cc: ControllerComponents, model: EventsModel, forms: FormsModel)(implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  case class CloneEdition(source: Int, target: Int)

  private implicit val createFormat = Json.reads[CloneEdition]

  def getEdition(id: Int): Action[AnyContent] = Action.async(model.getEdition(id).map {
    case Some(res) => Ok(Json.toJson(res))
    case _ => NotFound
  }).requiresAdmin

  def getStats(id: Int): Action[AnyContent] = Action.async(model.getEditionStats(id).map { res =>
    Ok(Json.toJson(res))
  }).requiresAdmin

  def getEditions: Action[AnyContent] =
    Action.async(model.getEditions.map(res => Ok(Json.toJson(res)))).requiresAdmin

  def updateMainForm(id: Int): Action[Int] = Action.async(parse.json[Int])(rq => {
    model.updateMainForm(id, rq.body).map(result => if (result) Ok else NotFound)
  }).requiresAdmin

  def cloneEdition: Action[CloneEdition] = Action.async(parse.json[CloneEdition])(rq => {
    forms.cloneEvent(rq.body.source, rq.body.target).map(_ => Ok)
  }).requiresAdmin

}
