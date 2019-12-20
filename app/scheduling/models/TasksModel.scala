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
          .groupBy(_._1).mapValues(_.map(_._2))
          .map { case (task, caps) => scheduling.Task(task.taskId, task.projectId, task.name, task.minAge, task.minExperience, caps.flatten.toList) }
          .toSeq
      )
  }

  def getTasks(project: Int): Future[Seq[scheduling.Task]] = {
    this.getTaskWithFilter(_.projectId === project)
  }

  def getTask(project: Int, task: Int): Future[Option[scheduling.Task]] = {
    this.getTaskWithFilter(t => t.projectId === project && t.id === task).map(seq => seq.headOption)
  }

  def createTask(task: Task, capabilities: Seq[Int]): Future[Int] = {
    db.run(
      (tasks.returning(tasks.map(_.id)) += task).flatMap(insertedId => {
        (taskCapabilities ++= capabilities.map(capId => (insertedId, capId))).map(_ => insertedId)
      })
    )
  }

  def updateTask(task: Task, capabilities: Seq[Int]): Future[_] = {
    db.run(
      (tasks.filter(_.id === task.taskId.get).update(task)).andThen({
        (taskCapabilities ++= capabilities.map(capId => (task.taskId.get, capId)))
      })
    )
  }

  /**
   * Returns a list of task slots for a project<br>
   *   The tasks embeded in the slots have no project and no capabilities included
   * @param project
   * @return
   */
  def getCompleteTaskSlots(project: Int, taskId: Int): Future[Seq[scheduling.TaskSlot]] = {
    db.run(tasks.filter(task => task.id === taskId && task.projectId === project)
      .join(taskSlots).on(_.id === _.taskId)
      .result)
      .map(list => list.map {
        case (task, slot) =>
          scheduling.TaskSlot(slot.taskSlotId.get, scheduling.Task(task.taskId, task.projectId, task.name, task.minAge, task.minExperience, Nil), slot.staffsRequired, slot.timeSlot)
      })
  }

  def getTaskSlots(project: Int, taskId: Int): Future[Seq[scheduling.models.TaskSlot]] =
    db.run(taskSlots.filter(_.taskId === taskId).sortBy(slot => (slot.day, slot.start, slot.end)).result)

}
