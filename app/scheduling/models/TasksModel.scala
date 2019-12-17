package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class TasksModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getTasks(project: Int): Future[Seq[scheduling.Task]] = {
    db.run(tasks.filter(_.projectId === project)
      .join(taskCapabilities).on(_.id === _.taskId)
      .join(capabilities).on(_._2.capabilityId === _.id)
      .map { case ((task, _), cap) => (task, cap.name) }
      .result)
      .map(list =>
        list
          .groupBy(_._1).mapValues(_.map(_._2))
          .map { case (task, caps) => scheduling.Task(task.taskId.get, null, task.name, task.minAge, task.minExperience, caps.toList) }
        .toSeq
      )
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
  def getTaskSlots(project: Int, taskId: Int): Future[Seq[scheduling.TaskSlot]] = {
    db.run(tasks.filter(task => task.id === taskId && task.projectId === project)
      .join(taskSlots).on(_.id === _.taskId)
      .result)
      .map(list => list.map {
        case (task, slot) =>
          scheduling.TaskSlot(slot.taskSlotId.get, scheduling.Task(task.taskId.get, null, task.name, task.minAge, task.minExperience, Nil), slot.staffsRequired, slot.timeSlot)
      })
  }
}
