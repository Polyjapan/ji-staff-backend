package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc.{Action, BodyParser, Request, Result}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
 * @author Louis Vialar
 */
class AppsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  private var apps: Set[String] = Set()
  private var nextRefresh = 0L

  private def refreshTokens = {
    if (System.currentTimeMillis() > nextRefresh)
      db.run(models.apps.map(_.appKey).result)
        .andThen {
        case Success(seq: Seq[String]) =>
          println("Refreshed authorized keys, " + seq)
          apps = seq.toSet
          nextRefresh = System.currentTimeMillis() + 5 * 60000L
      }
  }

  refreshTokens

  def checkAuthorized(token: Option[String]): Boolean = {
    // start a refresh if needed
    refreshTokens

    token
      .map(_.replaceAll("Bearer:", "").trim)
      .map(t => { println("Trying to login with '" + t + "'") ; t })
      .exists(apps)
    // checks that the token exists in apps
  }
}

object AppsModel {

  implicit class AppsAuthPostfix[T](action: Action[T])(implicit model: AppsModel) {
    def requiresApp: Action[T] = new Action[T] {
      override def parser: BodyParser[T] = action.parser

      override def apply(request: Request[T]): Future[Result] = {
        import play.api.mvc.Results._

        println("Headers " + request.headers + "; auth " + model.apps)
        if (model.checkAuthorized(request.headers.get("Authorization")
          .orElse(request.headers.get("authorization")))) action.apply(request)
        else Future(Unauthorized)(executionContext)
      }

      override def executionContext: ExecutionContext = action.executionContext
    }
  }

}
