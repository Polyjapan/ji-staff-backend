package data

import play.api.libs.json.{Format, Json}
import ch.japanimpact.auth.api.cas.CASServiceResponse

/**
  * @author Louis Vialar
  */
case class UserSession(userId: Int, email: String, groups: Set[String], firstName: String)

object UserSession {
  def apply(rep: CASServiceResponse): UserSession =
    UserSession(rep.user.toInt, rep.email.get, rep.groups, rep.firstname.get)

  implicit val format: Format[UserSession] = Json.format[UserSession]
}
