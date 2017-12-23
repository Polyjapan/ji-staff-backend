package data

import java.util.UUID

import play.api.libs.json.{JsObject, Json, OFormat}
import tools.DateUtils

/**
  * A class representing an application
  * @author Louis Vialar
  */
case class Application(userId: String, mail: String, year: String,
                        isValidated: Boolean = false, isAccepted: Boolean = false,
                        isRefused: Option[Boolean] = Option.apply(false),
                        validationDate: Option[Long] = Option.empty,
                        statusChangedBy: Option[(String, String)] = Option.empty,
                        content: JsObject = Json.obj(),
                        comments: Option[List[Comment]] = Option.empty,
                        claimToken: Option[String] = Option.empty
                      ) {

  /**
    * Helper method to create a new instance of this object changing only some fields
    */
  private def rebuild(userId: String = this.userId, mail: String = this.mail, year: String = this.year,
                      isValidated: Boolean = this.isValidated, isAccepted: Boolean = this.isAccepted,
                      isRefused: Option[Boolean] = this.isRefused,
                      validationDate: Option[Long] = this.validationDate,
                      statusChangedBy: Option[(String, String)] = this.statusChangedBy,
                      content: JsObject = this.content,
                      comments: Option[List[Comment]] = this.comments,
                      claimToken: Option[String] = this.claimToken) =
    Application(userId, mail, year, isValidated, isAccepted, isRefused, validationDate, statusChangedBy, content, comments, claimToken)

  /**
    * Try to update the content of this application with a provided content
    *
    * @param content the content to put in this application
    * @param bypassValidated if true, a validated application will still be modifiable
    * @return a pair (success, new application). The operation will fail only if this application is already validated
    */
  def withContent(content: JsObject, bypassValidated: Boolean = false): Option[Application] = {
    if (isValidated && !bypassValidated) Option.empty
    else Option apply rebuild(isValidated = false || bypassValidated, content = content)
  }

  /**
    * Returns an application in which any sensitive fields (like the one who accepted/refused or any comments made) are
    * excluded
    * @return this application, without sensitive fields
    */
  def removeSensitiveFields: Application = {
    // For now it removes the name of the person who accepted or refused the application
    rebuild(comments = Option.empty, claimToken = Option.empty, statusChangedBy = Option.empty)
  }

  def withComment(comment: Comment): Application = {
    rebuild(comments = Option.apply(comment :: comments.getOrElse(List())))
  }

  /**
    * Claim an unclaimed application (remove its claimToken and replace its userId and mail with actual values)
    * @param userId the id of the user claiming the application
    * @param mail the mail of the user claiming the application
    * @return an optional claimed Application, empty if the application cannot be claimed
    */
  def claimApplication(userId: String, mail: String): Option[Application] = {
    if (this.claimToken.isDefined && this.claimToken.get == this.userId && this.claimToken.get == this.mail)
      Option apply rebuild(userId = userId, mail = mail, claimToken = Option.empty)
    else
      Option.empty
  }

  /**
    * Mark an application as accepted
    * @param adminId the id of the admin accepting the application
    * @param adminName the name of the admin accepting the application
    * @param accepted true or false, depending if the application is set accepted or refused
    * @return
    */
  def accept(adminId: String, adminName: String, accepted: Boolean): Application = {
    rebuild(isAccepted = accepted, isRefused = Option.apply(!accepted), statusChangedBy = Option.apply((adminId, adminName)))
  }

  lazy val birthDateString: Option[String] = content.value.get("birthdate").flatMap(_.asOpt[String])

  /**
    * Try to validate this application. It will check it against the provided edition and if it is valid, will make return
    * a new validated instance of this application
    *
    * @param edition the edition to check
    * @return a triple (success, list of errors, application)
    */
  def validate(edition: Edition): (Boolean, List[String], Application) = {
    if (isValidated) (false, List("Candidature déjà validée"), this)
    else if (year != edition.year) (false, List("Année incorrecte"), this)
    else if (!edition.isActive) (false, List("Les inscriptions sont fermées pour cette édition"), this)
    else {
      val minor = birthDateString.map(DateUtils.extractDate(_, yearOffset = 18)).exists(_ after edition.conventionStart)
      val (succ, err, obj) = edition.verifyEditionAndBuildObject(content, minor)

      (succ, err, rebuild(isValidated = succ,
        validationDate = if (succ) Option.apply(System.currentTimeMillis) else Option.empty))
    }
  }

  /**
    * Return the same application but validated <br>
    *
    * WARNING this method doesn't perform any validation check. It should only be used for admin calls.
    */
  def setValidated: Application = rebuild(isValidated = true, validationDate = Option.apply(System.currentTimeMillis))

}

object Application {
  /**
    * The formatter used by the play framework Json library to convert json to/from applications
    */
  implicit val format: OFormat[Application] = Json.format[Application]

  /**
    * Creates an empty unclaimed application
    * @param year the year for which the application will be created
    * @param claimToken a custom claimToken if needed
    * @return the newly created application
    */
  def unclaimed(year: String, claimToken: String = UUID.randomUUID.toString) =
    new Application(claimToken, claimToken, year, isValidated = false, isAccepted = false,
      claimToken = Option apply claimToken)
}