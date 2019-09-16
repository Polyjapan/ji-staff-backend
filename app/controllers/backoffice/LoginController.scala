package controllers.backoffice

import ch.japanimpact.auth.api.AuthApi
import data.UserSession
import javax.inject.Inject
import pdi.jwt.JwtSession
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class LoginController @Inject()(cc: ControllerComponents, auth: AuthApi)(implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  def login(ticket: String): Action[AnyContent] = Action.async { implicit rq =>
    if (auth.isValidTicket(ticket)) {
      auth.getAppTicket(ticket).map {
        case Left(ticketResponse) if ticketResponse.ticketType.isValidLogin =>
          val session: JwtSession = JwtSession() + ("user", UserSession(ticketResponse))

          Ok(Json.toJson(Json.obj("session" -> session.serialize)))
        case Right(_) => BadRequest
      }
    } else Future(BadRequest)
  }

}
