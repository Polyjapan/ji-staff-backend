package models


import java.sql.Timestamp
import java.util.{Calendar, Date, GregorianCalendar}

import ch.japanimpact.auth.api.UsersApi
import data.ReturnTypes.ReducedUserData
import data.{StaffArrivalLog, StaffLogType}
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class LogsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, auth: UsersApi)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def addStaffLog(event: Int, staff: Int, logType: StaffLogType.Value) = {
    db.run(staffs.filter(s => s.eventId === event && s.staffNumber === staff).map(_.userId).result.headOption)
      .flatMap {
        case Some(userId) =>
          db.run(staffArrivalLogs += StaffArrivalLog(staff, event, None, logType)).flatMap(_ => {
            auth(userId).get.map(_.toOption)
          })
        case None => Future(None)
      }
  }

  def getMissingStaffs(event: Int, actionType: StaffLogType.Value) = {
    val cal = new GregorianCalendar()
    cal.setTime(new Date())

    if (cal.get(Calendar.HOUR_OF_DAY) < 5) {
      // Before 5 AM: show previous day
      cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    cal.set(Calendar.HOUR_OF_DAY, 5)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)

    val ts = new Timestamp(cal.getTimeInMillis)

    // Do query
    db.run {
      val staffsResult = staffs.filter(_.eventId === event).map(r => (r.staffNumber, r.userId)).result.map(_.toMap)
      val logsResult = staffArrivalLogs.filter(log => log.eventId === event && log.time > ts).map(log => (log.action, log.staffId)).result

      staffsResult.flatMap(allStaffs => logsResult.map(arrivedStaffs => {
        (allStaffs, arrivedStaffs)
      }))
    }.flatMap { case (allStaffs, logs) =>
      val (sameType, otherType) = logs.partition(_._1 == actionType)

      val missingStaffs = if (actionType == StaffLogType.Left) {
        // All the arrived staffs, minus the ones that have a log for this type
        val missingIds = otherType.map(_._2).toSet -- sameType.map(_._2).toSet

        missingIds.map(allStaffs) // get their actual user ids
      } else {
        // All the staffs, minus the ones that have a log for this type
        (allStaffs -- sameType.map(_._2).toSet).values.toSet
      }

      if (missingStaffs.isEmpty) {
        Future(Set.empty[ReducedUserData])
      } else {
        auth.getUsersWithIds(missingStaffs).map {
              // TODO: Opti is not good, this is O(n)
          case Right(res) => res.andThen(userData => ReducedUserData(userData)).elementWise.unapplySeq(missingStaffs.toSeq).get.toSet
          case _ => Set.empty[ReducedUserData]
        }
      }
    }.map(set => (cal.getTime, set))
  }
}

