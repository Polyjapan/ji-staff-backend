package tools

import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results._

/**
  * This class provides methods to map a future to a result
  * @author zyuiop
  */
trait FutureMappers {
  /**
    * Map an optional to a result. The result will be the [[Ok]] response holding a Json representation of the object if
    * the optional is defined, or a [[NotFound]] error if it's empty.
    * @param writes the Json converters available for the object type (usually not explicitly needed)
    * @tparam T the type of objects to handle
    * @return a result
    */
  def optionalMapper[T](implicit writes: Writes[T]): Option[T] => Result = {
    case Some(o) => Ok(Json.toJson(o))
    case None => NotFound
  }

  /**
    * Maps a list of objects to a 200 [[Ok]] result with a json representation of the list
    * @param writes the json converters
    * @tparam T the type of objects
    * @return a [[Ok]] response
    */
  def listMapper[T](implicit writes: Writes[T]): Seq[T] => Result = v => Ok(Json.toJson(v))
}
