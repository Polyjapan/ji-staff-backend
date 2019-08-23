package controllers.front

import data._
import data.Forms._
import data.Applications._
import javax.inject.{Inject, Singleton}
import models.{ApplicationsModel, AppsModel}
import models.AppsModel._
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, BodyParsers, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import models.ApplicationsModel.UpdateStateResult._
import models.ApplicationsModel._

/**
 * @author Louis Vialar
 */
@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents)(implicit apps: AppsModel, ec: ExecutionContext, applications: ApplicationsModel) extends AbstractController(cc) {
  def getState(form: Int, user: Int): Action[AnyContent] = Action.async {
    applications.getState(user, form).map {
      case Some(s) => Ok(Json.toJson(s))
      case None => NotFound
    }
  }.requiresApp

  def setState(form: Int, user: Int): Action[ApplicationState.Value] = Action.async(parse.json[ApplicationState.Value]) { v =>
    applications.updateState(user, form, v.body).map {
      case Success => Ok
      case NoSuchUser => NotFound
      case IllegalStateTransition => Forbidden
    }
  }.requiresApp

  case class FormReply(fieldId: Int, fieldValue: String)

  implicit val formReplyFormat: Format[FormReply] = Json.format[FormReply]

  def getReplies(form: Int, user: Int): Action[AnyContent] = Action.async {
    applications.getReplies(user, form).map(seq => Ok(Json.toJson(seq.map(FormReply.tupled))))
  }.requiresApp

  def postReplies(form: Int, user: Int): Action[List[FormReply]] = Action.async(parse.json[List[FormReply]]) { v =>
    applications.addReplies(user, form, v.body.map(FormReply.unapply).map(_.get)).map {
      case UpdateFieldsResult.Success => Ok
      case UpdateFieldsResult.ClosedApplication => MethodNotAllowed
      case UpdateFieldsResult.UnknownField => BadRequest
    }
  }.requiresApp

  def getPublicComments(form: Int, user: Int): Action[AnyContent] = Action.async {
    applications.getPublicComments(user, form).map {
      case Some(seq) => Ok(Json.toJson(seq.map(pair => Json.obj("time" -> pair._1, "comment" -> pair._2))))
      case None => NotFound
    }
  }.requiresApp
}
