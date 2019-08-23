package controllers.front

import data.User
import javax.inject.{Inject, Singleton}
import models.{AppsModel, UsersModel}
import models.AppsModel._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
@Singleton
class UserProfileController @Inject()(cc: ControllerComponents)(implicit apps: AppsModel, users: UsersModel, ec: ExecutionContext) extends AbstractController(cc) {
  def getProfile(user: Int): Action[AnyContent] = Action.async(users.getUser(user).map {
    case Some(user) => Ok(Json.toJson(user))
    case _ => NotFound
  }).requiresApp

  def updateProfile(user: Int): Action[User] = Action.async(parse.json[User]) { v =>
    users.updateUser(user, v.body).map {
      case success if success => Ok
      case _ => InternalServerError
    }
  }.requiresApp
}
