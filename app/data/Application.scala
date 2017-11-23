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
                        content: JsObject = Json.obj()) {
  /**
    * Try to update the content of this application with a provided content
    *
    * @param content the content to put in this application
    * @return a pair (success, new application). The operation will fail only if this application is already validated
    */
  def withContent(content: JsObject): (Boolean, Application) = {
    if (isValidated) (false, this)
    else (true, Application(userId, mail, year, isValidated = false, isAccepted, isRefused, validationDate,
      statusChangedBy, content))
  }

  def removeSensitiveFields: Application = {
    // For now it removes the name of the person who accepted or refused the application
    Application(userId, mail, year, isValidated, isAccepted, isRefused, validationDate, Option.empty, content)
  }

  def accept(adminId: String, adminName: String): Application = {
    Application(userId, mail, year, isValidated, true, Option.apply(false), validationDate, Option.apply((adminId, adminName)), content)
  }

  def refuse(adminId: String, adminName: String): Application = {
    Application(userId, mail, year, isValidated, false, Option.apply(true), validationDate, Option.apply((adminId, adminName)), content)
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