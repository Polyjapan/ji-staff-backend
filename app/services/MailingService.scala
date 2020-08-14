package services

import javax.inject.Inject
import play.api.Configuration
import play.twirl.api.Html
import ch.japanimpact.auth.api.{UserData, UserProfile, UsersApi}
import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class MailingService @Inject()(protected val mailer: MailerClient)(implicit ec: ExecutionContext, config: Configuration, users: UsersApi) {
  private def sendMail(userId: Int, title: String, content: UserData => Html) = {
    users(userId).get.map {
      case Right(userProfile) =>
        mailer.send(Email(
          "Japan Impact - " + title,
          "Staffs Japan Impact <noreply@japan-impact.ch>",
          Seq(userProfile.email),
          bodyHtml = Some(content(userProfile).body)
        ))
    }
  }

  def applicationAccept(userId: Int, eventName: String, staffNumber: Int): Future[String] =
    sendMail(userId, "Candidature acceptée", profile => views.html.emails.applicationAccepted(profile, eventName, staffNumber))

  def applicationRefuse(userId: Int, eventName: String): Future[String] =
    sendMail(userId, "Candidature rejetée", profile => views.html.emails.applicationRefused(profile, eventName))

  def formAccept(userId: Int, formName: String): Future[String] =
    sendMail(userId, "Formulaire " + formName + " validé", profile => views.html.emails.formAccepted(profile, formName))

  def formRequestChanges(userId: Int, formName: String): Future[String] =
    sendMail(userId, "Changements demandés sur le formulaire " + formName, profile => views.html.emails.formChangesRequested(profile, formName))

  def formRefuse(userId: Int, formName: String): Future[String] =
    sendMail(userId, "Formulaire " + formName + " refusé", profile => views.html.emails.formRefused(profile, formName))

  def formComment(userId: Int, formName: String, author: String, comment: String): Future[String] =
    sendMail(userId, "Nouveau commentaire sur le formulaire " + formName, profile => views.html.emails.newComment(profile, author, formName, comment))

  def formSent(userId: Int, formName: String, eventName: String): Future[String] = {
    users(userId).get.map {
      case Right(userProfile) =>
        mailer.send(Email(
          "Nouvelle réponse au formulaire " + formName,
          "Staffs Japan Impact <noreply@japan-impact.ch>",
          Seq("staff@japan-impact.ch"),
          bodyHtml = Some(views.html.emails.applicationSent(userProfile, formName, eventName).body)
        ))
    }
  }

}
