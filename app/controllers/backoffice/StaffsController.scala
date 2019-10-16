package controllers.backoffice

import ch.japanimpact.auth.api.{AuthApi, UserProfile}
import javax.inject.Inject
import models.StaffsModel
import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class StaffsController @Inject()(cc: ControllerComponents, auth: AuthApi, staffs: StaffsModel)
                                (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  case class StaffLine(staffNumber: Int, applicationId: Int, user: UserProfile)

  implicit val staffLineWrites: Writes[StaffLine] = Json.writes[StaffLine]

  def getStaffs(event: Int): Action[AnyContent] = Action.async({
    staffs.listStaffs(event).flatMap(list => {
      val staffIdMap = list.map(line => (line.user.userId, (line))).toMap

      auth.getUserProfiles(staffIdMap.keySet).map {
        case Left(seq) => Ok(Json.toJson(seq.map {
          case (k, v) =>
            val staff = staffIdMap(k)
            StaffLine(staff.staffNumber, staff.application, v)
        }))
        case Right(_) => InternalServerError
      }
    })
  }).requiresAuthentication
}
