package data

import play.api.libs.json.{JsObject, Json, OFormat}
import tools.DateUtils

/**
  * @author Louis Vialar
  */
case class Application(userId: String, mail: String, year: String,
                        isValidated: Boolean = false, isAccepted: Boolean = false,
                        isRefused: Option[Boolean] = Option.apply(false),
                        validationDate: Option[Long] = Option.empty,
                        statusChangedBy: Option[(String, String)] = Option.empty,
                        content: JsObject = Json.obj(),
                        comments: Option[List[String]] = Option.empty) {
  /**
    * Try to update the content of this application with a provided content
    *
    * @param content the content to put in this application
    * @param bypassValidated if true, a validated application will still be modifiable
    * @return a pair (success, new application). The operation will fail only if this application is already validated
    */
  def withContent(content: JsObject, bypassValidated: Boolean = false): (Boolean, Application) = {
    if (isValidated && !bypassValidated) (false, this)
    else (true, Application(userId, mail, year, isValidated = false, isAccepted, isRefused, validationDate,
      statusChangedBy, content))
  }

  /**
    * Returns an application in which any sensitive fields (like the one who accepted/refused or any comments made) are
    * excluded
    * @return this application, without sensitive fields
    */
  def removeSensitiveFields: Application = {
    // For now it removes the name of the person who accepted or refused the application
    Application(userId, mail, year, isValidated, isAccepted, isRefused, validationDate, Option.empty, content)
  }

  /**
    * Mark an application as accepted
    * @param adminId the id of the admin accepting the application
    * @param adminName the name of the admin accepting the application
    * @param accepted true or false, depending if the application is set accepted or refused
    * @return
    */
  def accept(adminId: String, adminName: String, accepted: Boolean): Application = {
    Application(userId, mail, year, isValidated, accepted, Option.apply(!accepted), validationDate, Option.apply((adminId, adminName)), content, comments)
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

      (succ, err, Application(userId, mail, year, isValidated = succ, isAccepted, isRefused,
        if (succ) Option.apply(System.currentTimeMillis) else Option.empty, statusChangedBy, obj))
    }
  }

}

object Application {
  implicit val format: OFormat[Application] = Json.format[Application]
}