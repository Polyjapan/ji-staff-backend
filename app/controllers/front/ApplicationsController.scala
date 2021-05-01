package controllers.front

import ch.japanimpact.auth.api
import ch.japanimpact.auth.api.apitokens.AuthorizationActions.OnlyApps
import ch.japanimpact.auth.api.apitokens.{App, AuthorizationActions}
import data.Applications._
import data.Forms.FormReply
import data._
import models.ApplicationsModel.UpdateStateResult._
import models.ApplicationsModel._
import models.{ApplicationsModel, EventsModel, FormsModel}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.MailingService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext, applications: ApplicationsModel, mailing: MailingService, forms: FormsModel, events: EventsModel, authorize: AuthorizationActions) extends AbstractController(cc) {
  private val stateParser = parse.tolerantText(30)
    .map(str => if (str.startsWith("\"") || str.startsWith("'")) str else "\"" + str + "\"")
    .map(str => Json.parse(str).as[ApplicationState.Value])

  def getState(form: Int, user: Int): Action[AnyContent] = authorize("staff/application/state/get").async {
    applications.getState(user, form).map {
      case Some(s) => Ok(Json.toJson(s))
      case None => NotFound
    }
  }

  def setState(form: Int, user: Int): Action[ApplicationState.Value] = authorize(OnlyApps, "staff/application/state/put").async(stateParser) { v =>
    applications.updateState(user, form, v.body).map {
      case Success =>
        if (v.body == ApplicationState.Sent) {
          forms.getForm(form).flatMap(form => {
            events.getEdition(form.get.eventId).flatMap(event => {
              mailing.formSent(user, form.get.name, event.get.name)
            })
          })
        }

        Ok
      case NoSuchUser => NotFound
      case IllegalStateTransition => Forbidden
    }
  }

  def getReplies(form: Int, user: Int): Action[AnyContent] = authorize("staff/application/replies/get").async {
    applications.getReplies(user, form).map(seq => Ok(Json.toJson(seq.map(FormReply.tupled))))
  }

  def postReplies(form: Int, user: Int): Action[List[FormReply]] = authorize(OnlyApps, "staff/application/replies/post").async(parse.json[List[FormReply]]) { v =>
    applications.addReplies(user, form, v.body.map(FormReply.unapply).map(_.get)).map {
      case UpdateFieldsResult.Success => Ok
      case UpdateFieldsResult.ClosedApplication => MethodNotAllowed
      case UpdateFieldsResult.UnknownField => BadRequest
    }
  }

  def getPublicComments(form: Int, user: Int): Action[AnyContent] = authorize(_ match {
    case App(_) => true
    case api.apitokens.User(id) => user == id
  }, _ match {
    case App(_) => Set("staff/application/comments/get")
    case _ => Set()
  }).async {
    applications.getPublicComments(user, form).map {
      case Some(seq) => Ok(Json.toJson(seq.map(pair => Json.obj("time" -> pair._1, "comment" -> pair._2))))
      case None => NotFound
    }
  }
}
