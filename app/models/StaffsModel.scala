package models

import java.sql.Date

import data.ReturnTypes.StaffingHistory
import data.{Forms, User}
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.Effect.Write
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class StaffsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, editions: EventsModel)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {

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

  case class Staff(staffNumber: Int, application: Int, user: User, level: Int, capabilities: List[String])

  def setLevels(event: Int, levels: List[(Int, Int)]): Future[List[Int]] = {
    db.run(DBIO.sequence(
      levels.map {
        case (staff, level) =>
          staffs.filter(s => s.staffNumber === staff && s.eventId === event)
            .map(_.staffLevel).update(level)

      }.asInstanceOf[List[DBIOAction[Int, NoStream, Write]]]
    ))
  }

  def addCapabilities(caps: List[(Int, Int, Int)]): Future[_] = {
    db.run(scheduling.models.staffCapabilities ++= caps)
  }


  def listStaffs(event: Int): Future[Seq[Staff]] = {
    val capabilitiesRequest = scheduling.models.capabilitiesMappingRequestForEvent(event)

    db.run(capabilitiesRequest.flatMap(capabilities => staffs
      .filter(_.eventId === event)
      .sortBy(_.staffNumber)
      .join(users).on(_.userId === _.userId)
      .join(applications).on(_._2.userId === _.userId)
      .map { case ((staff, user), application) => (staff.staffNumber, application.applicationId, user, staff.staffLevel) }
      .result
      .map(list => list.map(t4 => (t4._1, t4._2, t4._3, t4._4, capabilities(t4._1))).map(Staff.tupled)))
    )
  }

  def listStaffsDetails(eventId: Int): Future[(Seq[Forms.Field], Map[(Int, Int, Date), Seq[(Int, String)]])] = {
    db.run {
      forms.filter(f => f.eventId === eventId && f.isMain)
        .join(pages).on(_.formId === _.formId).map(_._2)
        .join(fields).on(_.formPageId === _.pageId)
        .sortBy { case ((page, field)) => page.ordering * 1000 + field.ordering }
        .result
        .map(seq => {
          val formId = seq.headOption.map(_._1.formId)
          val rest = seq.map(_._2)

          (formId.get, rest)
        })
        .flatMap[(Seq[data.Forms.Field], Map[(Int, Int, Date), Seq[(Int, String)]]), slick.dbio.NoStream, Effect.Read] {
          case (formId, fields) =>
            val fieldIds = fields.map(f => f.fieldId.get).toSet

            staffs.filter(_.eventId === eventId)
              .sortBy(_.staffNumber)
              .join(users).on(_.userId === _.userId)
              .join(applications).on((staff, app) => app.formId === formId && app.userId === staff._1.userId)
              .join(applicationsContents).on((l, r) => r.applicationId === l._2.applicationId)
              .map { case (((staff, user), _), content) => ((staff.staffNumber, staff.userId, user.birthDate), content.fieldId, content.value) }
              .result
              .map(seq => {
                seq.groupBy(_._1).view.mapValues(answers => {
                  val ans = answers
                    .map {
                      case (_, fieldId, value) => (fieldId, value)
                    }.filter(pair => fieldIds(pair._1))

                  ans
                }).toMap
              })
              .map(map => (fields, map))
        }
    }
  }

  def getStaffId(event: Int, user: Int): Future[Option[Int]] =
    db.run(staffs.filter(line => line.eventId === event && line.userId === user).map(_.staffNumber).result.headOption)


  def getStaffIdForCurrentEvent(user: Int): Future[Option[Int]] =
    editions.getCurrentEdition.flatMap {
      case Some(event) => getStaffId(event.id.get, user)
      case None => Future.successful(None)
    }

  def listStaffsForCurrentEvent: Future[Seq[Staff]] =
    editions.getCurrentEdition.flatMap {
      case Some(event) => listStaffs(event.id.get)
      case None => Future.successful(Seq.empty[Staff])
    }

  def getStaffings(user: Int): Future[Seq[StaffingHistory]] =
    db.run(
      staffs.filter(line => line.userId === user)
        .join(forms).on((l, r) => l.eventId === r.eventId && r.isMain)
        .join(applications).on((left, right) => left._2.formId === right.formId && left._1.userId === right.userId)
        .map { case ((staff, event), application) => (staff.staffNumber, application.applicationId, event.eventId) }
        .result
    ).flatMap { list =>
      // TODO: This sounds bad if there is no caching.
      val futures = list.map {
        case (staffNum, applId, eventId) =>
          editions.getEdition(eventId)
            .map(opt => opt.map(ev => StaffingHistory(staffNum, applId, ev)))
      }

      Future.sequence(futures).map(_.flatten)
    }
}

