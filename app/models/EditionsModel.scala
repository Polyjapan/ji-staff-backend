package models

import java.sql.Timestamp

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
}
