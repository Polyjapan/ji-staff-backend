package data

import ch.japanimpact.auth.api.{AppTicketResponse, TicketType}
import play.api.libs.json.{Format, Json}

/**
  * @author Louis Vialar
  */
case class UserSession(userId: Int, email: String, isApp: Boolean, groups: Set[String])

object UserSession {
  def apply(rep: AppTicketResponse): UserSession =
    UserSession(rep.userId, rep.userEmail, rep.ticketType == TicketType.AppTicket, rep.groups)

  implicit val format: Format[UserSession] = Json.format[UserSession]
}
