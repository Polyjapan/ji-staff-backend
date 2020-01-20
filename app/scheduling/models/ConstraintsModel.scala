package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scheduling.constraints
import scheduling.constraints.{AssociationConstraint, BannedTaskConstraint, BannedTaskTypeConstraint, FixedTaskConstraint, ScheduleConstraint, UnavailableConstraint}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class ConstraintsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getConstraints(projectId: Int): Future[Seq[constraints.ScheduleConstraint]] = {
    db.run {
      associationConstraints.filter(_.projectId === projectId).result flatMap { asso =>
        bannedTaskConstraints.filter(_.projectId === projectId).result flatMap { btc =>
          bannedTaskTypesConstraints.filter(_.projectId === projectId).result flatMap { bttc =>
            fixedTaskConstraints.filter(_.projectId === projectId).result flatMap { ftsc =>
              unavailableConstraints.filter(_.projectId === projectId).result map { uc => asso ++ btc ++ bttc ++ ftsc ++ uc }
            }
          }
        }
      }
    }
  }

  def createConstraint(projectId: Int, constraint: ScheduleConstraint) = {
    db.run {
      constraint match {
        case c: AssociationConstraint => associationConstraints += c
        case c: BannedTaskConstraint => bannedTaskConstraints += c
        case c: FixedTaskConstraint => fixedTaskConstraints += c
        case c: UnavailableConstraint => unavailableConstraints += c
        case c: BannedTaskTypeConstraint => bannedTaskTypesConstraints += c
      }
    }
  }

  def deleteConstraint(projectId: Int, constraint: ScheduleConstraint) = {
    db.run {
      constraint match {
        case c: AssociationConstraint => associationConstraints.filter(_.constraintId === c.constraintId).delete
        case c: BannedTaskConstraint => bannedTaskConstraints.filter(_.constraintId === c.constraintId).delete
        case c: FixedTaskConstraint => fixedTaskConstraints.filter(_.constraintId === c.constraintId).delete
        case c: UnavailableConstraint => unavailableConstraints.filter(_.constraintId === c.constraintId).delete
        case c: BannedTaskTypeConstraint => bannedTaskTypesConstraints.filter(_.constraintId === c.constraintId).delete
      }
    }
  }
}
