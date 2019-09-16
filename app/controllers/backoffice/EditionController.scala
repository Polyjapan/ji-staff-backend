package controllers.backoffice

import javax.inject.{Inject, Singleton}
import models.EditionsModel
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import data.AuthenticationPostfix._
import play.api.Configuration

/**
 * @author Louis Vialar
 */
@Singleton
class EditionController @Inject()(cc: ControllerComponents, model: EditionsModel)(implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  case class CreateEdition(name: String, date: java.sql.Date, copyOf: Option[Int])

  case class UpdateEdition(name: String, date: java.sql.Date)

  private implicit val createFormat = Json.reads[CreateEdition]
  private implicit val updateFormat = Json.reads[UpdateEdition]

  def getEdition(id: Int): Action[AnyContent] = Action.async(model.getEdition(id).map {
    case Some(res) => Ok(Json.toJson(res))
    case _ => NotFound
  }).requiresAuthentication

  def getEditions: Action[AnyContent] =
    Action.async(model.getEditions.map(res => Ok(Json.toJson(res)))).requiresAuthentication

  def createEdition: Action[CreateEdition] = Action.async(parse.json[CreateEdition])(rq => {
    ???
  }).requiresAuthentication

  def updateEdition(id: Int): Action[CreateEdition] = Action.async(parse.json[CreateEdition])(rq => {

  }).requiresAuthentication

}
