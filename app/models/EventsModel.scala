package models

import ch.japanimpact.api.APIError
import ch.japanimpact.api.events.EventsService
import ch.japanimpact.api.events.events.{Event, SimpleEvent}
import data.Applications.ApplicationState

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class EventsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, private val eventsApi: EventsService)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getCurrentEdition: Future[Option[SimpleEvent]] =
    eventsApi.getCurrentEvent().map(_.toOption.map(_.event))

  def getEditions: Future[Either[APIError, Iterable[SimpleEvent]]] =
    eventsApi.getEvents()
      .map(either => either.map(iterable => iterable.map(eventData => eventData.event)))
      //.map(_.toOption.getOrElse(Seq())).map(_.map(_.event))

  def getEdition(id: Int): Future[Option[SimpleEvent]] =
    eventsApi.getEvent(id).map(_.toOption.map(_.event))

  def getEditionStats(id: Int): Future[Map[ApplicationState.Value, Int]] =
    db.run(
      forms.filter(f => f.eventId === id && f.isMain)
        .join(applications).on(_.formId === _.formId)
        .map(_._2)
        .groupBy(_.state)
        .map { case (k, v) => (k, v.length) }
        .result
    ).map(pairs => pairs.toMap)

  def updateMainForm(id: Int, mainForm: Int): Future[Boolean] =
    db.run(
      DBIO.sequence(Vector(
        forms.filter(_.eventId === id).map(_.isMain).update(false),
        forms.filter(_.formId === mainForm).map(_.isMain).update(true)
      ))
    ).map(_(1) > 0)

}