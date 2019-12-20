package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scheduling.constraints.ScheduleConstraint
import slick.jdbc.MySQLProfile

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class SchedulingModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, val partitions: PartitionsModel)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._

  def pushSchedule(list: List[scheduling.StaffAssignation]): Future[_] = {
    db.run(staffsAssignation ++= list.map(a => StaffAssignation(a.taskSlot.id, a.user.user.userId)))
  }

  def buildSlotsForTask(project: Int, task: Int) = {
    buildSlots(partitions.getPartitionsForTask(project, task))
  }

  // Used when building the schedule, to ensure everything is generated correctly
  def buildSlotsForProject(project: Int) = {
    buildSlots(partitions.getPartitions(project))
  }

  private def buildSlots(slots: Future[Seq[TaskTimePartition]]) = {
    slots.flatMap {
      tasks => {
        val taskIds = tasks.map(part => part.task).toSet

        if (tasks.isEmpty) Future.successful(false)
        else db.run(
          taskSlots.filter(_.taskId.inSet(taskIds)).delete andThen
            (taskSlots ++= tasks.flatMap(_.produceSlots)).map(_ => true))
      }
    }
  }

  def getScheduleData(projectId: Int): Future[(scheduling.ScheduleProject, Seq[scheduling.Staff], immutable.Iterable[scheduling.TaskSlot], Seq[ScheduleConstraint])] = {
    db.run {
      // Type Inference in Slick is a bit buggy... We need to force it.
      val req: DBIOAction[(scheduling.ScheduleProject, Seq[scheduling.Staff], immutable.Iterable[scheduling.TaskSlot], Seq[ScheduleConstraint]), NoStream, Effect.Read] = scheduleProjects.filter(_.id === projectId)
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
                    task.taskId,
                    task.projectId,
                    task.name, task.minAge, task.minExperience, caps.map(_._2).toList
                  ), slot.staffsRequired, slot.timeSlot
                )
              }
            }

          val constraints =
            associationConstraints.filter(_.projectId === projectId).result flatMap { asso =>
            fixedTaskConstraints.filter(_.projectId === projectId).result flatMap { ftc =>
            fixedTaskSlotConstraints.filter(_.projectId === projectId).result flatMap { ftsc =>
            unavailableConstraints.filter(_.projectId === projectId).result map { uc => asso ++ ftc ++ ftsc ++ uc}}}}

          val proj = scheduling.ScheduleProject(project.projectId.get, event, project.projectTitle, project.maxTimePerStaff)


          staffs.flatMap(staffs => constraints.flatMap(constraints => slots.map(slots => (proj, staffs, slots, constraints))))
      }

      req
    }
  }
}
