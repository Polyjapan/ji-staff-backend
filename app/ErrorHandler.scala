import play.api.http.HttpErrorHandler
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import javax.inject.Singleton

import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * A class to convert all server and client errors to Json errors.
  */
@Singleton
class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Future.successful(
      Status(statusCode)(Json.toJson(Json.obj("code" -> statusCode, "messages" -> List(message))))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    exception.printStackTrace()
    Future.successful(
      InternalServerError(Json.toJson(Json.obj("code" -> 500, "messages" -> List(exception.getMessage))))
    )
  }
}