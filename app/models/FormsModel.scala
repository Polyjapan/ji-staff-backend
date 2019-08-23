package models

import java.sql.Timestamp

import data.Forms.{Field, Form}
import data._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class FormsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, editions: EditionsModel)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getMainForm: Future[Option[Form]] =
    db.run(
      editions.activeEventsWithMainForm
        .join(forms).on(_.mainForm.getOrElse(0) === _.formId)
        .map(_._2)
        .result
        .headOption)

  def getForm(id: Int): Future[Option[Form]] =
    db.run(
      forms.filter(_.formId === id)
        .result
        .headOption)

  def getForms: Future[Seq[Form]] =
    db.run(editions.activeEvents.map(_.eventId)
      .join(forms).on(_ === _.eventId)
      .map(_._2)
      .result)

  def getPages(form: Int): Future[Seq[Forms.FormPage]] =
    db.run(pages.filter(_.formId === form).result).map(_.sorted)

  def getPage(form: Int, page: Int): Future[Option[(Forms.FormPage, List[(Field, Map[String, String])])]] = {
    getPages(form).flatMap(pages => {
      if (pages.length >= page && page > 0) {
        val pg = pages(page - 1)

        db.run(
          fields.filter(_.pageId === pg.pageId)
            .joinLeft(fieldsAdditional).on(_.fieldId === _.fieldId)
            .result
        )
          .map(list => list.groupBy(_._1).mapValues(_.flatMap(_._2).toMap).toList)
          .map(list => Some(pg, list))
      } else Future(None)
    })
  }
}
