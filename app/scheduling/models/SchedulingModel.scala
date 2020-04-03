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

  private def getScheduleRaw(project: Int, staffId: Option[Int] = None, taskId: Option[Int] = None): Future[Map[Date, (Int, Int, Seq[(TaskSlot, String, StaffData)])]] = {
    db.run(scheduleProjects.filter(_.id === project).map(_.event).result.headOption)
      .flatMap {
        case None => Future.successful(Nil)
        case Some(eventId) => db.run(
          staffsAssignation
            .join(taskSlots).on((staff, slot) => {
            val result = staff.taskSlotId === slot.id

            if (taskId.isDefined)
              result && slot.taskId === taskId.get
            else result
          })
            .join(tasks).on((lhs, task) => lhs._2.taskId === task.id && task.projectId === project)
            .join(models.users).on(_._1._1.userId === _.userId)
            .join(models.staffs).on((line, staff) => {
            val result = line._2.userId === staff.userId && staff.eventId === eventId

            if (staffId.isDefined)
              result && staff.staffNumber === staffId.get
            else result
          })
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

  def getScheduleByStaff(project: Int, staffId: Option[Int] = None): Future[immutable.Iterable[ScheduleDay[StaffData, String]]] = {
    getScheduleRaw(project, staffId).map(result => {
      result.map {
        case (day, (minTime, maxTime, seq)) =>
          val columns = seq.groupBy(_._3)
            .mapValues(col => col.map { case (slot, task, _) => ScheduleLine(slot, task) })
            .map { case (staff, lines) => ScheduleColumn(staff, lines.toList) }

          ScheduleDay(day, minTime, maxTime, columns.toList.sortBy(_.header.staffNumber))
      }
    })
  }

  def getScheduleByTasks(project: Int, task: Option[Int] = None): Future[immutable.Iterable[ScheduleDay[String, StaffData]]] = {
    getScheduleRaw(project, None, task).map(result => {
      result.map {
        case (day, (minTime, maxTime, seq)) =>
          val columns = seq.groupBy(_._2)
            .mapValues(col => col.map { case (slot, _, staff) => ScheduleLine(slot, staff) })
            .flatMap { case (task, lines) =>
              val slots = lines.map(_.slot.timeSlot)
              val maxSim = slots.map(slot => slots.count(s2 => s2.isOverlapping(slot))).max

              if (maxSim <= 1) List(ScheduleColumn(task, lines.toList))
              else {
                def chooseSlots(remaining: List[ScheduleLine[StaffData]], i: Int = 1): List[ScheduleColumn[String, StaffData]] = {
                  if (remaining.isEmpty) Nil
                  else {
                    val (choosen, _) = scheduling.longestNonOverlappingSlot[ScheduleLine[StaffData]](remaining, line => line.slot.timeSlot)
                    val rest = remaining.filterNot(elem => choosen.contains(elem))

                    ScheduleColumn(task + " - " + i, choosen) :: chooseSlots(rest, i + 1)
                  }
                }

                chooseSlots(lines.toList)
              }
            }

          ScheduleDay(day, minTime, maxTime, columns.toList.sortBy(_.header))
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
                .join(models.users).on(_.userId === _.userId).map { case (staff, user) => (user, staff.staffLevel, staff.staffNumber) }
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
                        task.name, task.minAge, task.minExperience, caps.flatMap(_._2).toList,
                        task.taskType
                      ), slot.staffsRequired, slot.timeSlot
                    )
                  }
              }

            val constraints =
              associationConstraints.filter(_.projectId === projectId).result flatMap { asso =>
                bannedTaskConstraints.filter(_.projectId === projectId).result flatMap { btc =>
                  bannedTaskTypesConstraints.filter(_.projectId === projectId).result flatMap { bttc =>
                    fixedTaskConstraints.filter(_.projectId === projectId).result flatMap { ftsc =>
                      unavailableConstraints.filter(_.projectId === projectId).result map { uc => asso ++ btc ++ bttc ++ ftsc ++ uc }
                    }
                  }
                }
              }

            val proj = scheduling.ScheduleProject(project.projectId.get, event, project.projectTitle, project.maxTimePerStaff, project.minBreakMinutes, project.maxSameShiftType)


            staffs.flatMap(staffs => constraints.flatMap(constraints => slots.map(slots => (proj, staffs, slots, constraints))))
        }

      req
    }
  }
}
