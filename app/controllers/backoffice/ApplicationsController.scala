package controllers.backoffice

import java.sql.Date

import ch.japanimpact.auth.api.{AuthApi, UserProfile}
import data.Applications._
import data._
import javax.inject.{Inject, Singleton}
import models.ApplicationsModel
import models.ApplicationsModel.UpdateStateResult._
import play.api.Configuration
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.AuthenticationPostfix._
import utils.EnumUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents)(implicit conf: Configuration, ec: ExecutionContext, applications: ApplicationsModel, api: AuthApi) extends AbstractController(cc) {

  case class FilledPageField(field: data.Forms.Field, value: Option[String])

  case class FilledPage(page: Forms.FormPage, fields: Seq[FilledPageField])

  case class UserData(profile: UserProfile, birthDate: Date)

  case class ReducedUserData(firstName: String, lastName: String, email: String)

  object ReducedUserData {
    def apply(profile: UserProfile): ReducedUserData = ReducedUserData(profile.details.firstName, profile.details.lastName, profile.email)
  }

  case class ApplicationResult(user: UserData, state: ApplicationState.Value, content: Iterable[FilledPage])

  case class ApplicationListing(user: ReducedUserData, state: ApplicationState.Value, applicationId: Int)

  case class CommentWithAuthor(author: ReducedUserData, comment: ApplicationComment)

  implicit val userDataFormat: OFormat[UserData] = Json.format[UserData]
  implicit val reducedUserDataFormat: OFormat[ReducedUserData] = Json.format[ReducedUserData]
  implicit val listingFormat: OFormat[ApplicationListing] = Json.format[ApplicationListing]
  implicit val fieldFormat: OFormat[FilledPageField] = Json.format[FilledPageField]
  implicit val pageFormat: OFormat[FilledPage] = Json.format[FilledPage]
  implicit val resultFormat: OFormat[ApplicationResult] = Json.format[ApplicationResult]
  implicit val commentFormat: OFormat[CommentWithAuthor] = Json.format[CommentWithAuthor]

  def listApplications(form: Int, state: Option[String]): Action[AnyContent] = Action.async({
    applications.getApplications(form, state.map(v => EnumUtils.snakeNames(ApplicationState)(v)))
      .flatMap(applications => {
        api.getUserProfiles(applications.map(_._3.userId).toSet).map(map => (applications, map.left.get))
      })

      .map {
        case (applications, profiles) =>
          Ok(Json.toJson(applications.map {
            case (id, state, user) => ApplicationListing(ReducedUserData(profiles(user.userId)), state, id)
          }))
      }
  }).requiresAuthentication

  def getApplication(application: Int): Action[AnyContent] = Action.async({
    // Map[(data.User, Applications.ApplicationState.Value), Map[Forms.FormPage, Seq[(Forms.Field, String)]]]
    applications.getApplication(application)
      .map(res => res.headOption)
      .flatMap {
        case Some(((user, state), content))=>
          api.getUserProfile(user.userId).map(profile => {
            ApplicationResult(UserData(profile.left.get, user.birthDate), state, content.map {
              case (page, fields) => FilledPage(page, fields.map(FilledPageField.tupled))
            })
          }).map(p => Ok(Json.toJson(p)))

        case None => Future.successful(NotFound)
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
    applications
      .getAllComments(application)
      .flatMap { seq =>
          api.getUserProfiles(seq.map(_._2.userId).toSet).map(map => (seq, map.left.get))
      }
      .map {
        case (seq, profiles) =>
          seq.map { case (com, user) => CommentWithAuthor(ReducedUserData(profiles(user.userId)), com) }
      }
      .map(list => Ok(Json.toJson(list)))
  }).requiresAuthentication

  def addComment(application: Int): Action[ApplicationComment] = Action.async(parse.json[ApplicationComment])({ v =>
    val comment = v.body.copy(userId = v.user.userId, applicationId = application)

    applications.addComment(comment).map(res => if (res > 0) Ok else NotFound)
  }).requiresAuthentication
}
