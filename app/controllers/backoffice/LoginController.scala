package controllers.backoffice

import java.time.Clock

import ch.japanimpact.auth.api.AuthApi
import ch.japanimpact.auth.api.cas.CASService
import data.UserSession
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import pdi.jwt.JwtSession

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class LoginController @Inject()(cc: ControllerComponents, cas: CASService)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) {

  def login(ticket: String): Action[AnyContent] = Action.async { implicit rq =>
    cas.proxyValidate(ticket, None) map {
      case Left(err) =>
        println("CAS error: " + err)
        BadRequest
      case Right(data) =>
        val session: JwtSession = JwtSession() + ("user", UserSession(data))

        Ok(Json.toJson(Json.obj("session" -> session.serialize)))
    }
  }

}
