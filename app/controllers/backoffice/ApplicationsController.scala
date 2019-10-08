package controllers.backoffice

import data.Applications._
import data._
import javax.inject.{Inject, Singleton}
import models.ApplicationsModel
import models.ApplicationsModel.UpdateStateResult._
import play.api.Configuration
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.EnumUtils
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents)(implicit conf: Configuration, ec: ExecutionContext, applications: ApplicationsModel) extends AbstractController(cc) {

  case class FilledPageField(field: data.Forms.Field, value: Option[String])

  case class FilledPage(page: Forms.FormPage, fields: Seq[FilledPageField])

  case class ApplicationResult(user: User, state: ApplicationState.Value, content: Iterable[FilledPage])

  case class ApplicationListing(user: User, state: ApplicationState.Value, applicationId: Int)

  case class CommentWithAuthor(author: User, comment: ApplicationComment)

  implicit val listingFormat: OFormat[ApplicationListing] = Json.format[ApplicationListing]
  implicit val fieldFormat: OFormat[FilledPageField] = Json.format[FilledPageField]
  implicit val pageFormat: OFormat[FilledPage] = Json.format[FilledPage]
  implicit val resultFormat: OFormat[ApplicationResult] = Json.format[ApplicationResult]
  implicit val commentFormat: OFormat[CommentWithAuthor] = Json.format[CommentWithAuthor]

  def listApplications(form: Int, state: Option[String]): Action[AnyContent] = Action.async({
    applications.getApplications(form, state.map(v => EnumUtils.snakeNames(ApplicationState)(v))).map(res => Ok(Json.toJson(res.map {
      case (id, state, user) => ApplicationListing(user, state, id)
    })))
  }).requiresAuthentication

  def getApplication(application: Int): Action[AnyContent] = Action.async({
    // Map[(data.User, Applications.ApplicationState.Value), Map[Forms.FormPage, Seq[(Forms.Field, String)]]]
    applications.getApplication(application)
      .map(_.map {
        case ((user, state), content) => ApplicationResult(user, state, content.map {
          case (page, fields) => FilledPage(page, fields.map(FilledPageField.tupled))
        })
      }
      )
      .map(res => res.headOption)
      .map {
        case Some(res) => Ok(Json.toJson(res))
        case None => NotFound
      }
  }).requiresAuthentication

  def setState(applicationId: Int): Action[ApplicationState.Value] = Action.async(parse.json[ApplicationState.Value])({ v =>
    applications.updateStateByID(applicationId, v.body, privileged = true).map {
      case Success => Ok
      case NoSuchUser => NotFound
      case IllegalStateTransition => Forbidden
    }
  }).requiresAuthentication

  def getComments(application: Int): Action[AnyContent] = Action.async({
    applications.getAllComments(application).map(_.map { case (com, user) => CommentWithAuthor(user, com) }).map(list => Ok(Json.toJson(list)))
  }).requiresAuthentication

  def addComment(application: Int): Action[ApplicationComment] = Action.async(parse.json[ApplicationComment])({ v =>
    val comment = v.body.copy(userId = v.user.userId, applicationId = application)

    applications.addComment(comment).map(res => if (res > 0) Ok else NotFound)
  }).requiresAuthentication
}
