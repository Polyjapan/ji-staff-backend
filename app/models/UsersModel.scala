package models

import anorm._
import data._
import play.api.db.Database

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class UsersModel @Inject()(val database: Database)(implicit ec: ExecutionContext) {
  def getUser(id: Int): Future[Option[User]] = Future {
    database.withConnection(implicit conn => {
      SQL("SELECT * FROM users WHERE user_id = {id}")
        .on("id" -> id)
        .as(User.parser.singleOpt)
    })
  }

  def updateUser(userId: Int, content: User): Future[Boolean] = Future {
    database.withConnection(implicit c => {
      SqlUtils.replaceOne("users", "userId", content) == 1
    })
  }
}
