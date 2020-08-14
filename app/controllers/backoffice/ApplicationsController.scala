package controllers.backoffice

import java.io.ByteArrayOutputStream

import ch.japanimpact.api.events.EventsService
import ch.japanimpact.api.events.events.SimpleEvent
import ch.japanimpact.auth.api.UsersApi
import data.Applications._
import data.ReturnTypes._
import data._
import javax.inject.{Inject, Singleton}
import models.ApplicationsModel.UpdateStateResult._
import models.{ApplicationsModel, StaffsModel}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.MailingService
import utils.AuthenticationPostfix._
import utils.EnumUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents, mail: MailingService, staffs: StaffsModel, events: EventsService)
                                      (implicit conf: Configuration, ec: ExecutionContext,
                                       applications: ApplicationsModel, api: UsersApi) extends AbstractController(cc) {


  def listApplications(form: Int, state: Option[String]): Action[AnyContent] = Action.async({
    this.applications
      .getApplications(form, state.map(v => EnumUtils.snakeNames(ApplicationState)(v)))
      .flatMap { applications =>
        val userIds = applications.map(_._3.userId).toSet
        api.getUsersWithIds(userIds)
          .map {
            case Right(users) =>
              val list: Seq[ApplicationListing] = applications.flatMap {
                case (applicationId, applicationState, user) =>
                  users
                    .unapply(user.userId) // Get the optionnal user data
                    .map(userData => ApplicationListing(ReducedUserData(userData), applicationState, applicationId))
              }
              Ok(Json.toJson(list))
            case err =>
              println("API Error: " + err)
              InternalServerError
          }
      }
  }).requiresAdmin

  def getApplication(application: Int): Action[AnyContent] = Action.async({
    // Map[(data.User, Applications.ApplicationState.Value), Map[Forms.FormPage, Seq[(Forms.Field, String)]]]
    applications.getApplication(application)
      .map(res => res.headOption)
      .flatMap {
        case Some(((user, state), content)) =>
          api(user.userId).get.map(profile => {
            // TODO: Error handling...
            ApplicationResult(data.ReturnTypes.UserData.fromData(profile.toOption.get, user.birthDate), state, content.map {
              case (page, fields) => FilledPage(page, fields.map(FilledPageField.tupled))
            })
          }).map(p => Ok(Json.toJson(p)))

        case None => Future.successful(NotFound)
      }
  }).requiresAdmin


  def exportForm(form: Int): Action[AnyContent] = Action.async({
    applications.listReplies(form).flatMap {
      case Some((fields, ids, map)) =>
        val fieldsOrdering = fields.map(_.fieldId.get).zipWithIndex.toMap
        val userIds = map.keySet

        api.getUsersWithIds(userIds).map {
          case Right(profiles) =>

            val header =
              List("ID Staff") ++ List("Prénom") ++ List("Nom") ++ List("Email") ++ fields.map(_.name)

            val lines: Seq[Seq[String]] = header :: map.toList.sortBy(_._1).map {
              case (userId, content) =>
                val profile = profiles(userId)
                val staffId = ids.get(userId).map(i => i.toString).getOrElse("N/A")
                val missingIds = fieldsOrdering.keySet -- content.map(_._1).toSet

                val orderedContent = (content.toList ++ missingIds.map(id => (id, ""))).sortBy(pair => fieldsOrdering(pair._1))

                List(
                  staffId,
                  profile.details.firstName,
                  profile.details.lastName,
                  profile.email
                ) ::: orderedContent.map(_._2)
            }

            import com.github.tototoshi.csv._
            val os = new ByteArrayOutputStream()
            val csv = CSVWriter.open(os, "UTF-8")
            csv.writeAll(lines)
            csv.close()

            Ok(os.toString("UTF-8")).as("text/csv; charset=utf-8")

          case Left(_) => InternalServerError
        }
    }
  }).requiresAdmin


  private def sendStateMail(applicationId: Int, targetState: ApplicationState.Value) = {

    applications.getApplicationMeta(applicationId).flatMap {
      case (User(id, _), form) =>
        def eventFuture: Future[SimpleEvent] = events.getEvent(form.eventId).map(_.toOption.get.event)

        targetState match {
          case ApplicationState.Accepted if form.isMain =>
            staffs.getStaffId(form.eventId, id)
              .flatMap(staffNum =>
                eventFuture flatMap { event => mail.applicationAccept(id, event.name, staffNum.get) }
              )

          case ApplicationState.Accepted =>
            mail.formAccept(id, form.name)

          case ApplicationState.Refused if form.isMain =>
            eventFuture flatMap { event => mail.applicationRefuse(id, event.name) }

          case ApplicationState.Refused =>
            mail.formRefuse(id, form.name)

          case ApplicationState.RequestedChanges =>
            mail.formRequestChanges(id, form.name)
        }
    }
  }

  def setState(applicationId: Int): Action[ApplicationState.Value] = Action.async(parse.json[ApplicationState.Value])({ v =>
    applications.updateStateByID(applicationId, v.body, privileged = true)
      .map {
        case Success =>
          sendStateMail(applicationId, v.body)
          Ok
        case NoSuchUser => NotFound
        case IllegalStateTransition => Forbidden
      }
  }).requiresAdmin

  def getComments(application: Int): Action[AnyContent] = Action.async({
    applications
      .getAllComments(application)
      .flatMap { seq =>
        // TODO: Error handling
        api.getUsersWithIds(seq.map(_._2.userId).toSet).map(map => (seq, map.toOption.get))
      }
      .map {
        case (seq, profiles) =>
          seq.map { case (com, user) => CommentWithAuthor(ReducedUserData(profiles(user.userId)), com) }
      }
      .map(list => Ok(Json.toJson(list)))
  }).requiresAdmin

  def addComment(application: Int): Action[ApplicationComment] = Action.async(parse.json[ApplicationComment])({ v =>
    val comment = v.body.copy(userId = v.user.userId, applicationId = application)


    applications.addComment(comment).flatMap(res => {
      if (res > 0) {
        if (comment.userVisible) {
          applications.getApplicationMeta(application).flatMap {
            case (user, form) => mail.formComment(user.userId, form.name, v.user.firstName, comment.value)
          }.map(_ => Ok)
        } else Future.successful(Ok)
      } else Future.successful(NotFound)
    })
  }).requiresAdmin
}
