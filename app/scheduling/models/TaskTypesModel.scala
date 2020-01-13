package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class TaskTypesModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getTaskTypes: Future[Seq[(Int, String)]] = {
    db.run(taskTypes.map(t => (t.id, t.name)).result)
  }

  def createTaskType(value: String): Future[Int] = {
    db.run(taskTypes.returning(taskTypes.map(_.id)) += value)
  }
}
