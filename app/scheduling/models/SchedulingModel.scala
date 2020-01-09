package scheduling.models

import java.sql.Date

import ch.japanimpact.auth.api.AuthApi
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scheduling.constraints.ScheduleConstraint
import scheduling.{ScheduleColumn, ScheduleDay, ScheduleLine, StaffData}
import slick.jdbc.MySQLProfile

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class SchedulingModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, val partitions: PartitionsModel, val auth: AuthApi)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._

  def pushSchedule(list: List[scheduling.StaffAssignation]): Future[_] = {
    db.run(staffsAssignation ++= list.map(a => StaffAssignation(a.taskSlot.id, a.user.user.userId)))
  }

  private def getScheduleRaw(project: Int): Future[Map[Date, (Int, Int, Seq[(TaskSlot, String, StaffData)])]] = {
    db.run(scheduleProjects.filter(_.id === project).map(_.event).result.headOption)
      .flatMap {
        case None => Future.successful(Nil)
        case Some(eventId) => db.run(
          staffsAssignation
            .join(taskSlots).on(_.taskSlotId === _.id)
            .join(tasks).on((lhs, task) => lhs._2.taskId === task.id && task.projectId === project)
            .join(models.users).on(_._1._1.userId === _.userId)
            .join(models.staffs).on((line, staff) => line._2.userId === staff.userId && staff.eventId === eventId)
            .map {
              case ((((_, slot), task), user), staff) => ((task.name, slot), (user.userId, staff.staffNumber))
            }
            .result
        )
      }
      .flatMap { result =>
        val ids = result.map(_._2._1).toSet // user ids
        auth.getUserProfiles(ids)
          .map { a =>
            val map = a.left.getOrElse(Map())
            result.map {
              case ((task, slot), (userId, staffNumber)) =>
                (slot, task, StaffData(staffNumber, map.get(userId).map(u => u.details.firstName + " " + u.details.lastName).getOrElse("unknown")))
            }
          }
      }
      .map { result => {
        result
          .groupBy(_._1.timeSlot.day)
          .mapValues(seq => {
            val times = seq.map(_._1.timeSlot)
            (
              times.map(_.timeStart).min,
              times.map(_.timeEnd).max,
              seq
            )
          })
      }
      }
  }

  def getScheduleByStaff(project: Int): Future[immutable.Iterable[ScheduleDay[StaffData, String]]] = {
    getScheduleRaw(project).map(result => {
      result.map {
        case (day, (minTime, maxTime, seq)) =>
          val columns = seq.groupBy(_._3)
            .mapValues(col => col.map { case (slot, task, _) => ScheduleLine(slot, task) })
            .map { case (staff, lines) => ScheduleColumn(staff, lines.toList) }

          ScheduleDay(day, minTime, maxTime, columns.toList.sortBy(_.header.staffNumber))
      }
    })
  }

  def getScheduleByTasks(project: Int): Future[immutable.Iterable[ScheduleDay[String, StaffData]]] = {
    getScheduleRaw(project).map(result => {
      result.map {
        case (day, (minTime, maxTime, seq)) =>
          val columns = seq.groupBy(_._2)
            .mapValues(col => col.map { case (slot, _, staff) => ScheduleLine(slot, staff) })
            .map { case (task, lines) => ScheduleColumn(task, lines.toList) }

          ScheduleDay(day, minTime, maxTime, columns.toList)
      }
    })
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

  private val capsJoin = taskCapabilities.join(capabilities).on(_.capabilityId === _.id)

  def getScheduleData(projectId: Int): Future[(scheduling.ScheduleProject, Seq[scheduling.Staff], immutable.Iterable[scheduling.TaskSlot], Seq[ScheduleConstraint])] = {
    db.run {
      // Type Inference in Slick is a bit buggy... We need to force it.
      val req: DBIOAction[(scheduling.ScheduleProject, Seq[scheduling.Staff], immutable.Iterable[scheduling.TaskSlot], Seq[ScheduleConstraint]), NoStream, Effect.Read] = scheduleProjects.filter(_.id === projectId)
        .join(models.events).on(_.event === _.eventId)
        .result.head
        .flatMap {
          case (project, event) =>
            val staffCaps = capabilitiesMappingRequestForEvent(event.eventId.get)

            val staffs = staffCaps.flatMap(staffCapsMap => {
              models.staffs.filter(_.eventId === event.eventId)
                .join(models.users).on(_.userId === _.userId).map{ case (staff, user) => (user, staff.staffLevel, staff.staffNumber) }
                .result
                .map(_.map(pair => scheduling.Staff(pair._1, staffCapsMap(pair._3), pair._2, pair._1.ageAt(event.eventBegin))))
            })


            val slots = tasks.filter(task => task.projectId === projectId)
              .join(taskSlots).on { case (task, slot) => task.id === slot.taskId }
              .joinLeft(capsJoin).on { case ((task, _), (cap, _)) => task.id === cap.taskId }
              .result
              .map { lines =>
                lines
                  .map { case ((task, slot), cap) => ((slot, task), cap.map(_._2._2) /* cap name */ ) }
                  .groupBy(_._1)
                  .map { case ((slot, task), caps) =>
                    scheduling.TaskSlot(slot.taskSlotId.get,
                      scheduling.Task(
                        task.taskId,
                        task.projectId,
                        task.name, task.minAge, task.minExperience, caps.flatMap(_._2).toList
                      ), slot.staffsRequired, slot.timeSlot
                    )
                  }
              }

            val constraints =
              associationConstraints.filter(_.projectId === projectId).result flatMap { asso =>
                bannedTaskConstraints.filter(_.projectId === projectId).result flatMap { btc =>
                  fixedTaskConstraints.filter(_.projectId === projectId).result flatMap { ftsc =>
                    unavailableConstraints.filter(_.projectId === projectId).result map { uc => asso ++ btc ++ ftsc ++ uc }
                  }
                }
              }

            val proj = scheduling.ScheduleProject(project.projectId.get, event, project.projectTitle, project.maxTimePerStaff, project.minBreakMinutes)


            staffs.flatMap(staffs => constraints.flatMap(constraints => slots.map(slots => (proj, staffs, slots, constraints))))
        }

      req
    }
  }
}
