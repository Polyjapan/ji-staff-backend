package controllers.backoffice

import data.Applications._
import data.AuthenticationPostfix._
import data._
import javax.inject.{Inject, Singleton}
import models.ApplicationsModel
import models.ApplicationsModel.UpdateStateResult._
import play.api.Configuration
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import sun.jvm.hotspot.debugger.Page

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents)(implicit conf: Configuration, ec: ExecutionContext, applications: ApplicationsModel) extends AbstractController(cc) {

  case class FilledPageField(field: data.Forms.Field, value: String)

  case class FilledPage(page: Page, fields: List[FilledPageField])

  case class ApplicationResult(user: User, state: ApplicationState.Value, content: List[FilledPage])

  def listApplications(form: Int) = ??? // return an aggregated listing with <id, name of applicant>

  def getApplication(form: Int, user: Int) = ??? // return

  def getState(form: Int, user: Int): Action[AnyContent] = Action.async {
    applications.getState(user, form).map {
      case Some(s) => Ok(Json.toJson(s))
      case None => NotFound
    }
  }.requiresAuthentication

  def setState(form: Int, user: Int): Action[ApplicationState.Value] = Action.async(parse.json[ApplicationState.Value]) { v =>
    applications.updateState(user, form, v.body, privileged = true).map {
      case Success => Ok
      case NoSuchUser => NotFound
      case IllegalStateTransition => Forbidden
    }
  }.requiresAuthentication

  case class FormReply(fieldId: Int, fieldValue: String)

  implicit val formReplyFormat: Format[FormReply] = Json.format[FormReply]

  def getReplies(form: Int, user: Int): Action[AnyContent] = Action.async {
    applications.getReplies(user, form).map(seq => Ok(Json.toJson(seq.map(FormReply.tupled))))
  }.requiresAuthentication

  def getPublicComments(form: Int, user: Int): Action[AnyContent] = Action.async {
    applications.getPublicComments(user, form).map {
      case Some(seq) => Ok(Json.toJson(seq.map(pair => Json.obj("time" -> pair._1, "comment" -> pair._2))))
      case None => NotFound
    }
  }.requiresAuthentication
}
