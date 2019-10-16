package models

import data.ReturnTypes.StaffingHistory
import data.User
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class StaffsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def addStaff(event: Int, user: Int): Future[Int] = {
    db.run(
      staffs.filter(staff => staff.eventId === event && staff.userId === user).result.headOption.flatMap[Int, NoStream, Effect.All] {
        case None =>
          staffs.filter(_.eventId === event).map(_.staffNumber).max.result
            .map((num: Option[Int]) => (event, num.getOrElse(0) + 1, user))
            .flatMap(staffs += _) // Insert
        case Some(_) => DBIO.successful(1)
      }
    )
  }

  case class Staff(staffNumber: Int, application: Int, user: User)

  def listStaffs(event: Int): Future[Seq[Staff]] = {
    db.run(staffs
      .filter(_.eventId === event)
      .sortBy(_.staffNumber)
      .join(users).on(_.userId === _.userId)
      .join(applications).on(_._2.userId === _.userId)
      .map { case ((staff, user), application) => (staff.staffNumber, application.applicationId, user) }

      .result)
      .map(list => list.map(Staff.tupled))
  }

  def getStaffId(event: Int, user: Int): Future[Option[Int]] =
    db.run(staffs.filter(line => line.eventId === event && line.userId === user).map(_.staffNumber).result.headOption)


  def getStaffings(user: Int): Future[Seq[StaffingHistory]] =
    db.run(staffs.filter(line => line.userId === user)
      .join(events).on(_.eventId === _.eventId)
      .join(applications).on((left, right) => left._2.mainForm.get === right.formId && left._1.userId === right.userId)
      .map { case ((staff, event), application) => (staff.staffNumber, application.applicationId, event) }
      .result).map(_.map(StaffingHistory.tupled))
}

