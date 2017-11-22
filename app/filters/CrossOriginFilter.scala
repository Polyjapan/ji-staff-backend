package filters

import javax.inject._

import akka.stream.Materializer
import play.api.mvc._
import services.{AuthParserService, JwtCheckerService}

import scala.concurrent.{ExecutionContext, Future}

/**
  * This filter checks if the user is authenticated and adds the info in the request header
  */
@Singleton
class CrossOriginFilter @Inject()(
                                  implicit override val mat: Materializer,
                                  exec: ExecutionContext) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {
    nextFilter.apply(requestHeader).map(_.withHeaders("Access-Control-Allow-Origin" -> "*"))
  }

}
