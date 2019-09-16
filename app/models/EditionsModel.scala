package models

import java.sql.{Date, Timestamp}

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}
import data._

/**
 * @author Louis Vialar
 */
class EditionsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  private[models] val activeEvents = events
    .filter(_.isActive)
    .sortBy(_.eventBegin) // if multiple actives, the earliest comes first

  private[models] val activeEventsWithMainForm =
    activeEvents.filter(_.mainForm.isDefined)

  def getCurrentEdition: Future[Option[Event]] =
    db.run(activeEvents.result.headOption)

  def getEditions: Future[Seq[Event]] =
    db.run(events.result)

  def getEdition(id: Int): Future[Option[Event]] =
    db.run(events.filter(_.eventId === id).result.headOption)

  def updateNameAndDate(id: Int, name: String, date: Date): Future[Int] =
    db.run(events.filter(_.eventId === id).map(e => (e.name, e.eventBegin)).update((name, date)))

  def createEvent(name: String, date: Date): Future[Int] =
    db.run(events returning (events.map(_.eventId)) += Event(None, date, name, None, isActive = false))

}
