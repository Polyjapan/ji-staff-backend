package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class CapabilitiesModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getCapabilities: Future[Seq[(Int, String)]] = {
    db.run(capabilities.result)
  }

  def createCapability(value: String): Future[Int] = {
    db.run(capabilities.map(_.name).returning(capabilities.map(_.id)) += value)
  }
}
