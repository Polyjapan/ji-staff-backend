package models

import data.ReturnTypes.StaffingHistory
import data.{Forms, User}
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

  def deleteStaff(event: Int, user: Int): Future[Int] = {
    db.run(staffs.filter(staff => staff.eventId === event && staff.userId === user).delete)
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

  def listStaffsDetails(eventId: Int): Future[(Seq[Forms.Field], Map[(Int, Int), Seq[(Int, String)]])] = {
    db.run {
      events.filter(event => event.eventId === eventId)
        .join(forms).on(_.mainForm === _.formId).map(_._2)
        .join(pages).on(_.formId === _.formId).map(_._2)
        .join(fields).on(_.formPageId === _.pageId)
        .sortBy { case ((page, field)) => page.ordering * 1000 + field.ordering }
        .result
        .map(seq => {
          val formId = seq.headOption.map(_._1.formId)
          val rest = seq.map(_._2)

          (formId.get, rest)
        })
        .flatMap[(Seq[data.Forms.Field], Map[(Int, Int),Seq[(Int, String)]]),slick.dbio.NoStream,Effect.Read] {
          case (formId, fields) =>
            val fieldIds = fields.map(f => f.fieldId.get).toSet

            staffs.filter(_.eventId === eventId)
              .join(applications).on((staff, app) => app.formId === formId && app.userId === staff.userId)
              .join(applicationsContents).on((l, r) => r.applicationId === l._2.applicationId)
              .map { case ((staff, _), content) => ((staff.staffNumber, staff.userId), content.fieldId, content.value) }
              .result
              .map(seq => {
                seq.groupBy(_._1).mapValues(answers => {
                  val ans = answers
                    .map {
                      case (_, fieldId, value) => (fieldId, value)
                    }.filter(pair => fieldIds(pair._1))

                  val missingIds = ans.map(_._1).toSet -- fieldIds

                  ans ++ missingIds.map(id => (id, ""))
                })
              })
            .map(map => (fields, map))
        }
    }
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

