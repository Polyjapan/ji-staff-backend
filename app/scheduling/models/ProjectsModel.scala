package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class ProjectsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getProjects(event: Int): Future[Seq[scheduling.ScheduleProject]] = {
    db.run(scheduleProjects.filter(_.event === event).result)
      .map(list => list.map { proj => scheduling.ScheduleProject(proj.projectId.get, proj.event, proj.projectTitle, proj.maxTimePerStaff, proj.minBreakMinutes, proj.maxSameShiftType) })
  }

  def getAllProjects: Future[Map[Int, Seq[scheduling.models.ScheduleProject]]] = {
    db.run(scheduleProjects.result)
      .map(list => list.groupMap(_.event)(e => e))
  }

  def getProject(project: Int): Future[Option[scheduling.models.ScheduleProject]] = {
    db.run(scheduleProjects.filter(_.id === project).result.headOption)
  }

  def createProject(event: Int, name: String, maxHoursPerStaff: Int, minBreak: Int, maxSameShift: Int): Future[Int] = {
    db.run(scheduleProjects.returning(scheduleProjects.map(_.id)) += ScheduleProject(None, event, name, maxHoursPerStaff, minBreak, maxSameShift))
  }

  def cloneProject(source: Int, target: Int, cloneConstraints: Boolean = false) = {
    // Never clone generated stuff.
    // Only clone: constraints (4 tables), tasks (tasks + partitions)

    db.run(
      tasks.filter(task => task.projectId === source && task.deleted === false).result.flatMap(result => {
        val ids = result.map(_.taskId.get)
        ((tasks returning (tasks.map(_.id))) ++= result.map(_.copy(taskId = None, projectId = target)))
          .map(res => ids zip res).map(_.toMap)
      }).flatMap(idMap => {
        val partAndCaps = taskTimePartitions.filter(_.taskId.inSet(idMap.keys)).result
          .flatMap(result => taskTimePartitions ++= result.map(ttp => ttp.copy(taskPartitionId = None, task = idMap(ttp.task))))
          .andThen {
            taskCapabilities.filter(_.taskId.inSet(idMap.keys)).result
              .flatMap(r => taskCapabilities ++= r.map { case (task, cap) => (idMap(task), cap) })
          }

        if (cloneConstraints) {
          partAndCaps.andThen {
            associationConstraints.filter(_.projectId === source).result
              .flatMap(r => associationConstraints ++= r.map(_.copy(projectId = target)))
          }.andThen {
            unavailableConstraints.filter(_.projectId === source).result
              .flatMap(r => unavailableConstraints ++= r.map(_.copy(projectId = target)))
          }.andThen {
            bannedTaskConstraints.filter(_.projectId === source).result
              .flatMap(r => bannedTaskConstraints ++= r.map(c => c.copy(projectId = target, taskId = idMap(c.taskId))))
          }.andThen {
            fixedTaskConstraints.filter(_.projectId === source).result
              .flatMap(r => fixedTaskConstraints ++= r.map(c => c.copy(projectId = target, taskId = idMap(c.taskId))))
          }.andThen {
            bannedTaskTypesConstraints.filter(_.projectId === source).result
              .flatMap(r => bannedTaskTypesConstraints ++= r.map(c => c.copy(projectId = target)))
          }
        } else withSlots

      }))
  }
}
