package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class TasksModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._

  private val capsJoin = taskCapabilities.join(capabilities).on(_.capabilityId === _.id)

  private def getTaskWithFilter(predicate: Tasks => Rep[Boolean]): Future[Seq[scheduling.Task]] = {
    db.run(tasks
      .filter(predicate)
      .joinLeft(capsJoin).on(_.id === _._1.taskId)
      .result)
      .map(list =>
        list
          .map {
            case (task, Some((_, cap))) => (task, Some(cap._2))
            case (task, None) => (task, None)
          }
          .groupMap(_._1)(_._2)
          .map { case (task, caps) => scheduling.Task(task.taskId, task.projectId, task.name, task.minAge, task.minExperience, caps.flatten.toList, task.taskType) }
          .toSeq
      )
  }

  def deleteTask(project: Int, taskId: Int): Future[Int] =
    db.run(tasks.filter(t => t.projectId === project && t.id === taskId).map(_.deleted).update(true))

  def getTasks(project: Int): Future[Seq[scheduling.Task]] = {
    this.getTaskWithFilter(task => task.projectId === project && task.deleted === false)
  }

  def getTask(project: Int, task: Int): Future[Option[scheduling.Task]] = {
    this.getTaskWithFilter(t => t.projectId === project && t.id === task).map(seq => seq.headOption)
  }

  def createTask(task: Task, capabilities: Set[Int]): Future[Int] = {
    db.run(
      (tasks.returning(tasks.map(_.id)) += task).flatMap(insertedId => {
        (taskCapabilities ++= capabilities.map(capId => (insertedId, capId))).map(_ => insertedId)
      })
    )
  }

  def updateTask(task: Task, addCaps: Set[Int], removeCaps: Set[Int]): Future[_] = {
    db.run(
      (tasks.filter(_.id === task.taskId.get).update(task))
        .andThen(taskCapabilities ++= addCaps.map(capId => (task.taskId.get, capId)))
        .andThen(taskCapabilities.filter(c => c.taskId === task.taskId.get && c.capabilityId.inSet(removeCaps)).delete)
    )
  }

  def getTaskSlots(taskId: Int): Future[Seq[scheduling.models.TaskSlot]] =
    db.run(taskSlots.filter(_.taskId === taskId).sortBy(slot => (slot.day, slot.start, slot.end)).result)

}
