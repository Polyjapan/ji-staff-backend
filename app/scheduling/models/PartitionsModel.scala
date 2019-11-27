package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class PartitionsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getPartitions(project: Int): Future[Seq[scheduling.models.TaskTimePartition]] = {
    db.run(tasks.filter(_.projectId === project)
        .join(taskTimePartitions).on(_.id === _.taskId)
    )
    ???
  }
}
