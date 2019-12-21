package scheduling.models

import javax.inject.Inject
import models.{fields, fieldsAdditional, forms, pages}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class ProjectsModel@Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getProjects(event: Int): Future[Seq[scheduling.ScheduleProject]] = {
    db.run(scheduleProjects.filter(_.event === event).join(models.events).on(_.event === _.eventId).result)
      .map(list => list.map { case (proj, ev) => scheduling.ScheduleProject(proj.projectId.get, ev, proj.projectTitle, proj.maxTimePerStaff)})
  }

  def getAllProjects: Future[Map[data.Event, Seq[scheduling.models.ScheduleProject]]] = {
    db.run(scheduleProjects.join(models.events).on(_.event === _.eventId).result)
      .map(list => list
        .map { case (proj, ev) => (ev, scheduling.models.ScheduleProject(proj.projectId, ev.eventId.get, proj.projectTitle, proj.maxTimePerStaff)) }
        .groupBy(_._1)
        .mapValues(_.map(_._2))
      )
  }

  def createProject(event: Int, name: String, maxHoursPerStaff: Int): Future[Int] = {
    db.run(scheduleProjects.returning(scheduleProjects.map(_.id)) += ScheduleProject(None, event, name, maxHoursPerStaff))
  }

  def cloneProject(source: Int, target: Int, cloneSlots: Boolean = false, cloneConstraints: Boolean = false) = {
    // Never clone generated stuff.
    // Only clone: constraints (4 tables), tasks (tasks + partitions)

    db.run(
      tasks.filter(_.projectId === source).result.flatMap(result => {
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

        val withSlots = if (cloneSlots) {
          val slots = partAndCaps.andThen {
            taskSlots.filter(_.taskId.inSet(idMap.keys)).result
              .flatMap(r =>
                ((taskSlots returning (taskSlots.map(_.id))) ++= r.map(l => l.copy(taskSlotId = None, taskId = idMap(l.taskId))))
                  .map(res => (r.map(_.taskSlotId.get)) zip res).map(_.toMap)
              )
          }

          if (cloneConstraints) {
            slots.flatMap { slotsMap =>
              fixedTaskSlotConstraints.filter(_.slotId.inSet(slotsMap.keys)).result
                  .flatMap(constraints => fixedTaskSlotConstraints ++= constraints.map(c => c.copy(projectId = target, slotId = slotsMap(c.slotId))))
            }
          } else slots
        } else partAndCaps

        if (cloneConstraints) {
          withSlots.andThen {
            associationConstraints.filter(_.projectId === source).result
              .flatMap(r => associationConstraints ++= r.map(_.copy(projectId = target)))
          }.andThen {
            unavailableConstraints.filter(_.projectId === source).result
              .flatMap(r => unavailableConstraints ++= r.map(_.copy(projectId = target)))
          }.andThen {
            bannedTaskConstraints.filter(_.projectId === source).result
              .flatMap(r => bannedTaskConstraints ++= r.map(c => c.copy(projectId = target, taskId = idMap(c.taskId))))
          }
        } else withSlots

      }))
  }
}
