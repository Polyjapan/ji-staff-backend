package models

import java.sql.Timestamp

import data._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc.Result
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class UsersModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._

  def getUser(id: Int): Future[Option[User]] =
    db.run(users.filter(_.userId === id).result.headOption)

  def updateUser(userId: Int, content: User): Future[Boolean] = {
    val user = content.copy(userId = userId)
    db.run(users.insertOrUpdate(user).map(_ > 0))
  }

}
