package controllers.backoffice

import javax.inject.{Inject, Singleton}
import models.{EditionsModel, FormsModel}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import data.AuthenticationPostfix._
import play.api.Configuration

/**
 * @author Louis Vialar
 */
@Singleton
class EditionController @Inject()(cc: ControllerComponents, model: EditionsModel, forms: FormsModel)(implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

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
    // 1st we create the edition, whatever happens
    model.createEvent(rq.body.name, rq.body.date).flatMap(evId => {
      if (rq.body.copyOf.isDefined) {
        forms.cloneEvent(rq.body.copyOf.get, evId).map(_ => evId)
      } else {
        Future.successful(evId)
      }
    }).map(evId => Ok(Json.toJson(evId)))
  }).requiresAuthentication

  def updateEdition(id: Int): Action[CreateEdition] = Action.async(parse.json[CreateEdition])(rq => {
    model.updateNameAndDate(id, rq.body.name, rq.body.date).map(updated => if (updated > 0) Ok else NotFound)
  }).requiresAuthentication

  def updateActiveStatus(id: Int): Action[Boolean] = Action.async(parse.text(7).map(value => value == "true"))(rq => {
    model.updateActive(id, rq.body).map(updated => if (updated > 0) Ok else NotFound)
  }).requiresAuthentication

  def updateMainForm(id: Int): Action[Int] = Action.async(parse.json[Int])(rq => {
    model.updateMainForm(id, rq.body).map(updated => if (updated > 0) Ok else NotFound)
  }).requiresAuthentication

}
