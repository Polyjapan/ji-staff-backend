package controllers.front

import ch.japanimpact.auth.api.apitokens.AuthorizationActions
import ch.japanimpact.auth.api.apitokens.AuthorizationActions.OnlyApps
import data.User

import javax.inject.{Inject, Singleton}
import models.UsersModel
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
@Singleton
class UserProfileController @Inject()(cc: ControllerComponents)(implicit users: UsersModel, ec: ExecutionContext, authorize: AuthorizationActions) extends AbstractController(cc) {
  def getProfile(user: Int): Action[AnyContent] = authorize("staff/profile/get").async(users.getUser(user).map {
    case Some(user) => Ok(Json.toJson(user))
    case _ => NotFound
  })

  def updateProfile(user: Int): Action[User] = authorize(OnlyApps, "staff/profile/put").async(parse.json[User]) { v =>
    users.updateUser(user, v.body).map {
      case success if success => Ok
      case _ => InternalServerError
    }
  }
}
