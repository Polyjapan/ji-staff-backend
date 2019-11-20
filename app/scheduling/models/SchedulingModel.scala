package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scheduling.constraints.ScheduleConstraint
import slick.jdbc.MySQLProfile

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class SchedulingModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._

  def getTimePartitions(project: Int): Future[Seq[TaskTimePartition]] = {
    db.run(
        tasks.filter(task => task.projectId === project)
        .join(taskTimePartitions).on { case (task, ttp) => ttp.taskId === task.id }
        .map { case (task, ttp) => ttp }
        .result
    )
  }

  def pushSchedule(list: List[scheduling.StaffAssignation]): Future[_] = {
    db.run(staffsAssignation ++= list.map(a => StaffAssignation(a.taskSlot.id, a.user.user.userId)))
  }

  /**
   * This will take all the [[TaskTimePartition]] objects, produce the associated [[TaskSlot]], and push them to the database
   * @param project the project on which the operation should be done
   * @return
   */
  def buildSlots(project: Int) = {
    getTimePartitions(project).flatMap {
      tasks => db.run(taskSlots ++= tasks.flatMap(_.produceSlots))
    }
  }

  def getScheduleData(projectId: Int): Future[(scheduling.ScheduleProject, Seq[scheduling.Staff], immutable.Iterable[scheduling.TaskSlot], Seq[ScheduleConstraint])] = {
    db.run(scheduleProjects.filter(_.id === projectId)
        .join(models.events).on(_.event === _.eventId)
        .result.head
        .flatMap {
        case (project, event) =>
          val staffs = models.staffs.filter(_.eventId === event.eventId)
            .join(models.users).on(_.userId === _.userId).map(_._2)
            .result
            .map(_.map(user => scheduling.Staff(user, Nil, 0, user.ageAt(event.eventBegin)))) // TODO: Skill and abilities

          val slots = tasks.filter(task => task.projectId === projectId)
            .join(taskSlots).on { case (task, slot) => task.id === slot.taskId }
            .join(taskCapabilities).on { case ((task, _), cap) => task.id === cap.taskId }
            .join(capabilities).on { case ((_, tCap), cap) => tCap.capabilityId === cap.id }
            .map { case (((task, slot), _), cap) => ((slot, task), cap.name) }
            .result
            .map { lines =>
              lines.groupBy(_._1).map { case ((slot, task), caps) =>
                scheduling.TaskSlot(slot.taskSlotId.get,
                  scheduling.Task(
                    scheduling.ScheduleProject(project.projectId.get, event, project.projectTitle, project.maxTimePerStaff),
                    task.name, task.minAge, task.minExperience, caps.map(_._2).toList
                  ), slot.staffsRequired, slot.timeSlot
                )
              }
            }

          val proj = scheduling.ScheduleProject(project.projectId.get, event, project.projectTitle, project.maxTimePerStaff)
            staffs.flatMap(staffs => slots.map(slots => (proj, staffs, slots, Nil))) // TODO: constraints
      })
  }
}
