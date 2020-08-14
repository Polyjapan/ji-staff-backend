package controllers.backoffice

import java.text.SimpleDateFormat

import ch.japanimpact.auth.api.UserProfile
import data.ReturnTypes.ReducedUserData
import data.StaffLogType
import javax.inject.Inject
import models.LogsModel
import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, Action, ControllerComponents, Result}
import utils.AuthenticationPostfix._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class LogsController @Inject()(cc: ControllerComponents, logs: LogsModel)
                              (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  case class StaffLine(staffNumber: Int, applicationId: Int, user: UserProfile, level: Int, capabilities: List[String])

  implicit val staffLineWrites: Writes[StaffLine] = Json.writes[StaffLine]

  def arrived(event: Int): Action[Int] = Action.async(parse.text(10).map(_.toInt)) { req =>
    logs.addStaffLog(event, req.body, StaffLogType.Arrived).map { case Some(prof) => Ok(Json.toJson(ReducedUserData(prof))) case None => NotFound }
  }.requiresAdmin

  def left(event: Int): Action[Int] = Action.async(parse.text(10).map(_.toInt)) { req =>
    logs.addStaffLog(event, req.body, StaffLogType.Left).map { case Some(prof) => Ok(Json.toJson(ReducedUserData(prof))) case None => NotFound }
  }.requiresAdmin

  private def missingStaffs(event: Int, eventType: StaffLogType.Value): Future[Result] = {
    logs.getMissingStaffs(event, eventType).map {
      case (date, log) => Ok(Json.obj("time" -> new SimpleDateFormat().format(date), "missing" -> Json.toJson(log)))
    }
  }

  def notArrived(event: Int) = Action.async {
    missingStaffs(event, StaffLogType.Arrived)
  }.requiresAdmin

  def notLeft(event: Int) = Action.async {
    missingStaffs(event, StaffLogType.Left)
  }.requiresAdmin


}
