package tools

import play.api.libs.json.Json
import play.api.mvc.{Result, _}

import scala.util.{Failure, Success, Try}

/**
  * @author Louis Vialar
  */
object Tools {
  def jsonTransformer: Try[Any] => Try[Result]  = {
    case Success(null) => Try.apply(Results.NotFound)
    case Success(seq) => Try(Results.Ok(Json.toJson(seq)))
    case Failure(t) => Try(Results.InternalServerError(t.getMessage))
  }
}
