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
        .map(_._2)
        .result
    )
  }

  def getPartitionsForTask(project: Int, task: Int): Future[Seq[scheduling.models.TaskTimePartition]] = {
    db.run(
      taskTimePartitions.filter(_.taskId === task)
        .sortBy(_.periodOrdering)
        .result)
  }

  def createPartition(partition: TaskTimePartition): Future[Int] = {
    db.run(taskTimePartitions.returning(taskTimePartitions.map(_.id)) += partition)
  }

  def createPartitions(partitions: Seq[TaskTimePartition]): Future[_] = {
    db.run(taskTimePartitions ++= partitions)
  }

  def updatePartition(partition: TaskTimePartition): Future[_] = {
    db.run(taskTimePartitions.filter(p => p.taskId === partition.task && p.id === partition.taskPartitionId.get).update(partition))
  }

  def deletePartition(task: Int, partition: Int): Future[_] = {
    db.run(taskTimePartitions.filter(p => p.id === partition && p.taskId === task).delete)
  }

}
