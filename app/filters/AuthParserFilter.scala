package filters

import javax.inject._

import akka.stream.Materializer
import play.api.mvc._
import services.{JwtCheckerService, AuthParserService}

import scala.concurrent.{ExecutionContext, Future}

/**
  * This filter checks if the user is authenticated and adds the info in the request header
  */
@Singleton
class AuthParserFilter @Inject()(
                                  implicit override val mat: Materializer,
                                  authService: JwtCheckerService,
                                  authHeadersService: AuthParserService,
                                  exec: ExecutionContext) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader.withAttrs(authHeadersService.getAttributes(requestHeader.headers)))
  }

}
