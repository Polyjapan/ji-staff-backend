package tools

import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results._

/**
  * @author zyuiop
  */
trait FutureMappers {
  def optionalMapper[T](implicit writes: Writes[T]): Option[T] => Result = {
    case Some(o) => Ok(Json.toJson(o))
    case None => NotFound
  }

  def listMapper[T](implicit writes: Writes[T]): Seq[T] => Result = v => Ok(Json.toJson(v))
}
