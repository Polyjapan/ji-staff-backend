package models

import java.sql.Timestamp

import data._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile
import Applications.ApplicationState._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
 * @author Louis Vialar
 */
class ApplicationsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._
  import ApplicationsModel.UpdateStateResult
  import ApplicationsModel.UpdateFieldsResult

  def updateState(user: Int, form: Int, state: Applications.ApplicationState.Value, privileged: Boolean = false): Future[UpdateStateResult.Value] = {
    // Get existing
    db.run(applications.filter(a => a.userId === user && a.formId === form).result.headOption).flatMap {
      case None if state == Applications.ApplicationState.Draft || privileged =>
        db.run((applications += (None, user, form, state)).asTry).map {
          case Success(_) => UpdateStateResult.Success
          case _ => UpdateStateResult.NoSuchUser // constraint failed
        }
      case None => Future.successful(UpdateStateResult.IllegalStateTransition)
      case Some((Some(id), _, _, currentState)) =>
        val allowed = (currentState, state) match {
          case (Draft, Sent) => true
          case (Sent, Draft) => true
          case (RequestedChanges, Draft) => true
          case _ => privileged
        }

        if (!allowed) Future.successful(UpdateStateResult.IllegalStateTransition)
        else db.run(applications.filter(_.applicationId === id).map(_.state).update(state))
          .map(i => UpdateStateResult.Success)
    }
  }

  def getState(user: Int, form: Int): Future[Option[Applications.ApplicationState.Value]] =
    db.run(applications.filter(a => a.userId === user && a.formId === form).map(_.state).result.headOption)

  def getReplies(user: Int, form: Int): Future[Seq[(Int, String)]] =
    db.run(
      applications.filter(a => a.userId === user && a.formId === form)
        .map(_.applicationId)
        .join(applicationsContents).on(_ === _.applicationId)
        .map(_._2)
        .map(tuple => (tuple.fieldId, tuple.value))
        .result)

  def addReplies(user: Int, form: Int, replies: Seq[(Int, String)]): Future[UpdateFieldsResult.Value] = {
    if (replies.isEmpty) Future.successful(UpdateFieldsResult.Success)
    else {
      // 1. check fields
      db.run(
        pages.filter(_.formId === form).map(_.formPageId)
          .join(fields).on(_ === _.pageId).map(_._2.fieldId)
          .result
      )
        .map(fields => fields.toSet)
        .map(fields => replies.map(_._1).forall(fields)) // all the fields in replies are in the form
        .flatMap(contains => {
          if (contains) {
            db.run(
              applications
                .filter(a => a.userId === user && a.formId === form)
                .result.headOption
                .flatMap {
                  case None => applications.returning(applications.map(_.applicationId)) += (None, user, form, Draft)
                  case Some((Some(id), _, _, state)) =>
                    if (state == Draft) DBIO.successful(id)
                    else DBIO.failed(new IllegalStateException())
                }
                .flatMap(id => {
                  val queries = replies.map(rep => applicationsContents.insertOrUpdate((id, rep._1, rep._2)))
                  DBIO.sequence(queries)
                })
                .asTry)
              .map(app => if (app.isSuccess) UpdateFieldsResult.Success else UpdateFieldsResult.ClosedApplication)
          } else {
            Future.successful(UpdateFieldsResult.UnknownField)
          }
      })
    }
  }

  def getPublicComments(user: Int, form: Int): Future[Option[Seq[(Timestamp, String)]]] = {
    db.run(applications.filter(a => a.userId === user && a.formId === form)
      .map(_.applicationId)
      .joinLeft(applicationsComments).on((id, comment) => comment.applicationId === id && comment.userVisible)
      .map(pair => (pair._1, pair._2.map(com => (com.timestamp, com.value))))
      .result)
      .map(seq =>
        if (seq.isEmpty) None
        else Some(seq
          .filterNot(_._2.isEmpty)
          .map(_._2.get))
      )
  }


}

object ApplicationsModel {

  object UpdateStateResult extends Enumeration {
    val Success, NoSuchUser, IllegalStateTransition = Value
  }

  object UpdateFieldsResult extends Enumeration {
    val Success, ClosedApplication, UnknownField = Value
  }

}