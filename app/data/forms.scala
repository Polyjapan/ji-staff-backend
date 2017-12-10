package data

import play.api.libs.json._
import tools.DateUtils

/**
  * A package containing classes and tools about forms
  */
package object forms {

  /**
    * A class describing a form field
    *
    * @author Louis Vialar
    */
  case class FormField(key: String,
                       label: String,
                       helpText: String = "",
                       controlType: String = "text",
                       required: Boolean = true,
                       order: Int = Int.MaxValue,
                       additionalData: JsObject = Json.obj(),
                       validators: List[JsObject] = List()
                      ) {
    val convertedValidators: List[FormFieldValidator] = validators.map(FormFieldValidator.fromJson)

    /**
      * Returns a new FormField with the provided new validators in addition of those already in this formfield
      * @param newValidators a list of validators to add to the current field
      * @return a new FormField with the new validators provided
      */
    def withValidators(newValidators: List[FormFieldValidator]): FormField = {
      FormField(key, label, helpText, controlType, required, order,
        additionalData, validators ::: newValidators.map(_.toJson))
    }

    /**
      * Returns a new FormField with the provided new validator in addition of those already in this formfield
      * @param validator the validator to add to the current field
      * @return a new FormField
      */
    def withValidator(validator: FormFieldValidator): FormField = withValidators(List(validator))

    /**
      * Check if a given value passes the different validators
      *
      * @param value the value to test
      * @return a pair (passes, list of errors messages)
      */
    def verifyValue(value: Option[JsValue]): (Boolean, List[String]) = {
      if (value.isEmpty && required) (false, List("Le champ " + label + " est requis"))
      else if (value.isEmpty) (true, List())
      else {
        val lst = convertedValidators.foldLeft(List[String]())(
          (list, validator) =>
            if (validator.validateInput(value.get)) list
            else (validator.message) :: list
        )
        (lst.isEmpty, lst)
      }
    }
  }

  object FormField {
    implicit val format: OFormat[FormField] = Json.format[FormField]
  }

  case class FormPage(pageNumber: Int, minorOnly: Boolean, title: String, fields: List[FormField]) {
    /**
      * Check that the page is correct and insert correct values in the given object
      * @param data the data received from the user that we want to check
      * @param objToBuild the existing object we want to modify
      * @return a triple (success, list of errors, built object)
      */
    def verifyPageAndBuildObject(data: JsObject, objToBuild: JsObject): (Boolean, List[String], JsObject) = {
      fields.foldLeft((true, List[String](), objToBuild))(
        (pair, entry) => (pair, entry) match {
          // We map the pair (list of errors, source js object) and the current entry to usable names
          case ((success: Boolean, list: List[String], source: JsObject), field) =>
            val value = data.value.get(field.key)
            (field.verifyValue(value), value) match {
              case ((true, _), Some(v)) => (success, list, source + (field.key, v))
              case ((true, _), None) => (success, list, source)
              case ((false, er), _) => (false, list ::: er, source)
            }
        }
      )
    }
  }

  object FormPage {
    implicit val format: OFormat[FormPage] = Json.format[FormPage]
  }

  /**
    * A class that can validate that a field has valid data
    */
  class FormFieldValidator(val message: String, val jsonType: String) {
    def validateInput(input: JsValue): Boolean = false

    def toJson: JsObject = Json.obj("type" -> jsonType, "message" -> message);
  }

  object FormFieldValidator {
    def fromJson(doc: JsObject): FormFieldValidator = doc("type").as[String] match {
      case "regex" => new StringRegexValidator(doc)
      case "intbounds" => new IntegerValueValidator(doc)
      case "date" => new DateValidator(doc)
      case "set" => new SetContainedValidator(doc)
      case _ => throw new IllegalArgumentException("Trying to convert from to unregistered validator")
    }
  }

  case class StringRegexValidator(override val message: String, regex: String) extends FormFieldValidator(message, "regex") {
    def this(doc: JsObject) = this(doc("message").as[String], doc("regex").as[String])

    override def validateInput(input: JsValue): Boolean = input.asOpt[String] match {
      case Some(input: String) => input.matches(regex)
      case None => false
    }

    override def toJson: JsObject = super.toJson + ("regex" -> JsString(regex))
  }

  case class SetContainedValidator(override val message: String, validValues: Set[String]) extends FormFieldValidator(message, "set") {
    def this(doc: JsObject) = this(doc("message").as[String], doc("validValues").as[Set[String]])

    override def validateInput(input: JsValue): Boolean = input.asOpt[String] match {
      case Some(input: String) => validValues(input)
      case None => false
    }

    override def toJson: JsObject = super.toJson + ("validValues" -> JsArray(validValues.map(JsString).toList))
  }

  case class IntegerValueValidator(override val message: String, min: Int, max: Int) extends FormFieldValidator(message, "intbounds") {
    def this(doc: JsObject) = this(doc("message").as[String], doc("min").as[Int], doc("max").as[Int])

    override def validateInput(input: JsValue): Boolean = input.asOpt[Int] match {
      case Some(input: Int) => input >= min && input <= max
      case _ => false
    }

    override def toJson: JsObject = super.toJson + ("min" -> JsNumber(min)) + ("max" -> JsNumber(max))
  }


  case class DateValidator(override val message: String) extends FormFieldValidator(message, "date") {
    def this(doc: JsObject) = this(doc("message").as[String])

    override def validateInput(input: JsValue): Boolean = input.asOpt[String] match {
      case Some(input: String) => DateUtils.isValidDate(input)
      case _ => false
    }
  }


}