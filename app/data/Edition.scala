package data

import java.util.Date

import data.forms.FormPage
import play.api.libs.json.{JsObject, Json, OFormat}

/**
  * @author Louis Vialar
  */
case class Edition(year: String, applicationsStart: Date, applicationsEnd: Date, conventionStart: Date, formData: List[FormPage]) {
  def isActive: Boolean = {
    val today = new Date()
    (today before applicationsEnd) && (today after applicationsStart)
  }

  /**
    * Check that the form has correctly been filled and fill a JsObject with all the required data
    *
    * @param data the data received from the user that we want to check
    * @param isMinor whether the user is minor and has to fill the minor only page
    * @return a triple (success, list of errors, built object)
    */
  def verifyEditionAndBuildObject(data: JsObject, isMinor: Boolean): (Boolean, List[String], JsObject) = {
    formData
      .filter(!_.minorOnly || isMinor) // take only the pages that our user has to fill
      .foldLeft((true, List[String](), Json.obj()))(
      (pair, entry) => (pair, entry) match {
        // We map the pair (list of errors, source js object) and the current entry to usable names
        case ((success: Boolean, list: List[String], source: JsObject), page) =>
           page.verifyPageAndBuildObject(data, source) match {
            case (true, _, obj) => (success, list, source ++ obj)
            case (false, er, _) => (false, list ::: er, source)
          }
      }
    )
  }
}

object Edition {
  implicit val format: OFormat[Edition] = Json.format[Edition]
}
