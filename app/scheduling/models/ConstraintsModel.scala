package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scheduling.constraints
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class ConstraintsModel@Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getConstraints(projectId: Int): Future[Seq[constraints.ScheduleConstraint]] = {
    db.run {
        associationConstraints.filter(_.projectId === projectId).result flatMap { asso =>
          bannedTaskConstraints.filter(_.projectId === projectId).result flatMap { btc =>
            fixedTaskSlotConstraints.filter(_.projectId === projectId).result flatMap { ftsc =>
              unavailableConstraints.filter(_.projectId === projectId).result map { uc => asso ++ btc ++ ftsc ++ uc }
            }
          }
        }
    }
  }

  def createConstraint(projectId: Int ) = ???
}
