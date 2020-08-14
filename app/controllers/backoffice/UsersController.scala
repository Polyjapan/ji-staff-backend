package controllers.backoffice

import ch.japanimpact.auth.api.UsersApi
import data.ReturnTypes._
import javax.inject.Inject
import models.{ApplicationsModel, StaffsModel}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.AuthenticationPostfix._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class UsersController @Inject()(cc: ControllerComponents, users: UsersApi, staffs: StaffsModel, apps: ApplicationsModel)
                               (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {


  def getHistory(user: Int): Action[AnyContent] = Action.async({
    users(user).get.flatMap {
      case Right(profile) =>

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

      case _ => Future.successful(NotFound)
    }
  }).requiresAdmin
}
