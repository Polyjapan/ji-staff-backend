package utils

import play.api.libs.json._
import slick.relational.RelationalProfile

/**
 * @author Louis Vialar
 */
object EnumUtils {
  def snakeNames[T <: Enumeration](T: T): Map[String, T.Value] =
    T.values
      .map(v => (toSnakeName(v), v))
      .toMap

  def toSnakeName(v: Any): String =
    v.toString.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase


  def format[T <: Enumeration](T: T): Format[T.Value] = {
    lazy val snakeNames: Map[String, T.Value] = this.snakeNames(T)

    new Format[T.Value] {
      override def reads(json: JsValue): JsResult[T.Value] = json match {
        case JsString(str) => JsSuccess(snakeNames(str))
        case _ => JsError("Invalid type")
      }

      override def writes(o: T.Value): JsValue = JsString(toSnakeName(o))
    }
  }

  def methodMap[T <: Enumeration](T: T, profile: RelationalProfile): profile.BaseColumnType[T.Value] = {
    import profile.api._
    lazy val snakeNames: Map[String, T.Value] = this.snakeNames(T)

    MappedColumnType.base[T.Value, String](toSnakeName, snakeNames)
  }
}
