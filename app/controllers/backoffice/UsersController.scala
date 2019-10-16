package controllers.backoffice

import ch.japanimpact.auth.api.AuthApi
import javax.inject.Inject
import models.{ApplicationsModel, StaffsModel}
import play.api.Configuration
import play.api.libs.json.{Format, Json}
import data.ReturnTypes._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.AuthenticationPostfix._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class UsersController @Inject()(cc: ControllerComponents, auth: AuthApi, staffs: StaffsModel, apps: ApplicationsModel)
                               (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {



  def getHistory(user: Int): Action[AnyContent] = Action.async({
    auth.getUserProfile(user).flatMap {
      case Left(profile) =>

        staffs.getStaffings(user).flatMap(staffings => {
          apps.getApplicationsForUser(user).map(applications => {
            Ok(Json.toJson(
              Json.obj(
                "profile" -> profile,
                "staffings" -> staffings,
                "applications" -> applications
              )
            ))
          })
        })

      case Right(_) => Future.successful(NotFound)
    }
  }).requiresAuthentication
}
