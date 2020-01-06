package models

import java.sql.Timestamp

import data.Applications.ApplicationComment
import data.Applications.ApplicationState._
import data.ReturnTypes.ApplicationHistory
import data.{Applications, Forms, User}
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
 * @author Louis Vialar
 */
class ApplicationsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, staffsModel: StaffsModel)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {


  import ApplicationsModel.{UpdateFieldsResult, UpdateStateResult}
  import profile.api._


  def updateStateByID(applicationId: Int, body: Applications.ApplicationState.Value, privileged: Boolean = false) =
    doUpdateState(app => app.applicationId === applicationId, body, privileged)

  def updateState(user: Int, form: Int, body: Applications.ApplicationState.Value, privileged: Boolean = false) =
    doUpdateState(app => app.formId === form && app.userId === user, body, privileged, () => {
      db.run((applications += (None, user, form, body)).asTry).map {
        case Success(_) => UpdateStateResult.Success
        case _ => UpdateStateResult.NoSuchUser // constraint failed
      }
    })

  private def doUpdateState(filter: Applications => Rep[Boolean], state: Applications.ApplicationState.Value, privileged: Boolean = false, tryCreate: () => Future[UpdateStateResult.Value] = () => Future.successful(UpdateStateResult.IllegalStateTransition)): Future[UpdateStateResult.Value] = {
    def doUpdate(id: Int) = db.run(applications.filter(_.applicationId === id).map(_.state).update(state))

    // Get existing
    db.run(applications.filter(filter).result.headOption).flatMap {
      case None if state == Applications.ApplicationState.Draft || privileged => tryCreate()
      case None => Future.successful(UpdateStateResult.IllegalStateTransition)
      case Some((Some(id), _, _, currentState)) =>
        val allowed = (currentState, state) match {
          case (Draft, Sent) => true
          case (Sent, Draft) => true
          case (RequestedChanges, Draft) => true
          case _ => privileged
        }

        if (!allowed) Future.successful(UpdateStateResult.IllegalStateTransition)
        else {
          if (state == Accepted || currentState == Accepted) {
            db.run(applications.filter(_.applicationId === id).join(events).on(_.formId === _.mainForm).map(pair => (pair._1.userId, pair._2.eventId)).result.headOption).flatMap {
              case Some((userId, eventId)) =>
                if (state == Accepted)
                  staffsModel.addStaff(eventId, userId).flatMap(i => doUpdate(id))
                else // was accepted before _> no longer staff
                  staffsModel.deleteStaff(eventId, userId).flatMap(_ => doUpdate(id))
              case None =>
                // This is not the main form
                doUpdate(id)
            }
          } else {
            doUpdate(id)
          }
          }.map(i => UpdateStateResult.Success)
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


  def getAllComments(application: Int): Future[Seq[(ApplicationComment, User)]] = {
    db.run(applicationsComments.filter(_.applicationId === application).join(users).on(_.userId === _.userId).result)
  }

  def listReplies(formId: Int): Future[Option[(Seq[Forms.Field], Map[Int, Int], Map[Int, Seq[(Int, String)]])]] = {
    db.run {
      forms.filter(_.formId === formId).result.headOption.flatMap[Option[(Seq[data.Forms.Field], Map[Int,Int], Map[Int,Seq[(Int, String)]])],slick.dbio.NoStream,Effect.Read] {
        case Some(form) =>
          // Get the staff IDs for the event
          events.filter(event => event.eventId === form.eventId)
            .join(staffs).on((event, staff) => event.eventId === staff.eventId)
            .map { case (_, staff) => (staff.userId, staff.staffNumber) }
            .result
            .map(seq => seq.toMap)
            .flatMap(staffMap => {
              pages.filter(_.formId === form.formId)
                .join(fields).on(_.formPageId === _.pageId)
                .sortBy { case ((page, field)) => page.ordering * 1000 + field.ordering }
                .map(_._2) // keep only fields
                .result
                .flatMap(fields => {
                  val fieldIds = fields.map(f => f.fieldId.get).toSet

                  applications.filter(app => app.formId === formId)
                    .join(applicationsContents).on((l, r) => r.applicationId === l.applicationId)
                    .map { case (application, content) => (application.userId, content.fieldId, content.value) }
                    .result
                    .map(seq => {
                      seq.groupBy(_._1).mapValues(_.map {
                        case (_, fieldId, value) => (fieldId, value)
                      }.filter(pair => fieldIds(pair._1)) // Keep only fields that are in the form
                      )
                    })
                    .map(map => Some((fields, staffMap, map)))

                })
            })
        case _ => DBIO.successful(None)
      }
    }
  }

  def getApplications(form: Int, state: Option[Applications.ApplicationState.Value]): Future[Seq[(Int, Applications.ApplicationState.Value, data.User)]] = {
    val filtered = applications.filter(app => app.formId === form)
    db.run(
      (if (state.isEmpty) filtered else filtered.filter(_.state === state.get))
        .join(users).on(_.userId === _.userId)
        .map { case (l, r) => (l.applicationId, l.state, r) }
        .result)
  }

  def getApplicationMeta(application: Int): Future[(data.User, data.Forms.Form, data.Event)] =
    db.run(applications.filter(_.applicationId === application)
      .join(users).on(_.userId === _.userId)
      .join(forms).on(_._1.formId === _.formId)
      .join(events).on(_._2.eventId === _.eventId)
      .map { case (((_, user), form), event) => (user, form, event) }
      .result.head)

  def getApplication(application: Int): Future[Map[(data.User, Applications.ApplicationState.Value), Map[Forms.FormPage, Seq[(Forms.Field, Option[String])]]]] = {
    db.run(
      applications
        .filter(_.applicationId === application)
        .join(users).on(_.userId === _.userId)
        .join(pages).on(_._1.formId === _.formId)
        .join(fields).on(_._2.formPageId === _.pageId)
        .joinLeft(applicationsContents).on((l, r) => l._2.fieldId === r.fieldId && r.applicationId === application)
        .map {
          case ((((app, user), page), field), content) => ((user, app.state), (page, (field, content.map(_.value))))
        }
        .result
    ).map(_
      .groupBy(_._1) // group by (user, state) pair
      .mapValues(_
        .map(_._2)
        .groupBy(_._1) // group by page
        .mapValues(_.map(_._2)) // keep field-value pairs
      )
    )
  }

  def addComment(comment: ApplicationComment) = db.run(applicationsComments += comment)

  def getApplicationsForUser(user: Int): Future[Seq[ApplicationHistory]] =
    db.run(applications.filter(_.userId === user)
      .join(forms).on(_.formId === _.formId)
      .join(events).on(_._2.eventId === _.eventId)
      .map { case ((app, form), ev) => (app.applicationId, app.state, form, ev) }
      .result
    ).map(_.map(ApplicationHistory.tupled))
}

object ApplicationsModel {

  object UpdateStateResult extends Enumeration {
    val Success, NoSuchUser, IllegalStateTransition = Value
  }

  object UpdateFieldsResult extends Enumeration {
    val Success, ClosedApplication, UnknownField = Value
  }

}