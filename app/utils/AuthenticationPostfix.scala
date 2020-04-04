package utils

import java.time.Clock

import data.UserSession
import play.api.Configuration
import play.api.mvc._
import pdi.jwt.JwtSession._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Utility class to remove some boilerplate regarding authentication on the endpoints
 *
 * @author Louis Vialar
 */
object AuthenticationPostfix {

  /**
   * Defines a handler of authentication.
   * It takes a potential user and returns a boolean: true if the user is authorized, false if not. It can also
   * provide a result that will be returned in case the user is not authorized.
   */
  abstract class AuthorizationHandler extends Function[Option[UserSession], Boolean] {
    def andAlso(other: AuthorizationHandler): AuthorizationHandler = (user: Option[UserSession]) => {
      val self = this (user)
      if (self) other(user) // This handler authorized the user, check that the next authorizes it too
      else self // This handler refused the user, return its result
    }
  }

  object AuthorizationHandler {
    val ensuringAuthentication: AuthorizationHandler = (user: Option[UserSession]) => user.isDefined

    def ensuringGroup(group: String): AuthorizationHandler = ensuringAuthentication.andAlso((user: Option[UserSession]) => user.get.groups.contains(group))
  }

  case class AuthenticationAction[T](action: Action[T], handler: AuthorizationHandler)(implicit conf: Configuration) extends Action[T] {
    override def apply(request: Request[T]): Future[Result] = {
      val user = request.optUser
      val result = handler(user)

      import play.api.mvc.Results._

      if (result) action(request) // call the parent action, knowing we are authenticated
      else Future.successful(Unauthorized) // return an error
    }

    override def parser: BodyParser[T] = action.parser

    override def executionContext: ExecutionContext = action.executionContext
  }

  implicit class AuthenticationPostfix[T](action: Action[T]) {
    def requiresAdmin(implicit conf: Configuration): Action[T] = requiresGroup("resp-staffs")

    def requiresGroup(group: String)(implicit conf: Configuration): Action[T] = AuthenticationAction(action, AuthorizationHandler.ensuringGroup(group))
  }

  implicit class UserRequestHeader(request: RequestHeader)(implicit conf: Configuration) {
    implicit val clock = Clock.systemUTC()

    private def session = Some(request.jwtSession).filter(_.claim.isValid)

    def optUser: Option[UserSession] = session.flatMap(_.getAs[UserSession]("user"))

    def user: UserSession = optUser.get
  }

}
