package models

import data.User
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Format, Json}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class StaffsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def addStaff(event: Int, user: Int): Future[Int] = {
    db.run(
      staffs.filter(staff => staff.eventId === event && staff.userId === user).result.headOption.flatMap {
        case None =>
          staffs.filter(_.eventId === event).map(_.staffNumber).max.result
            .map((num: Option[Int]) => (event, num.getOrElse(0) + 1, user))
            .flatMap(staffs += _) // Insert
        case Some(_) => DBIO.successful(1)
      }
    )
  }

  case class Staff(staffNumber: Int, user: User)

  implicit def format: Format[Staff] = Json.format[Staff]

  def listStaffs(event: Int): Future[Seq[Staff]] = {
    db.run(staffs.filter(_.eventId === event).join(users).on(_.userId === _.userId).result)
      .map(list => list.map { case ((_, staffNum, _), user) => Staff(staffNum, user) })
  }
}

