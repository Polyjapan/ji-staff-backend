package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class VersionsModel@Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  // Version creation is done at [[SchedulingModel]].

  def getVersions(project: Int): Future[Seq[scheduling.ScheduleVersion]] = {
    db.run(scheduleVersions.filter(_.projectId === project).sortBy(_.id.desc).result)
  }

  def setActive(project: Int, version: Int): Future[Vector[Int]] = {
    db.run(
      DBIO.sequence(Vector(
        scheduleVersions.filter(_.projectId === project).map(_.visible).update(false),
        scheduleVersions.filter(v => v.projectId === project && v.id === version).map(_.visible).update(true)
      ))
    )
  }

  def setTag(project: Int, version: Int, tag: Option[String]): Future[Int] = {
    db.run(scheduleVersions.filter(v => v.projectId === project && v.id === version).map(_.versionTag).update(tag))
  }
}
