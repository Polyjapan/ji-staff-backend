package models

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

  def cloneEvent(source: Int, target: Int): Future[Option[Int]] =
  // List forms
    db.run(
      forms.filter(_.eventId === source).result.flatMap(
        formsList => {
          val ids = formsList.map(_.formId.get)

          ((forms returning (forms.map(_.formId))) ++= formsList.map(f => f.copy(formId = None, eventId = target)))
            .map(res => ids.zip(res).toMap)
            .map(res => (ids, res))
        })
        .flatMap { case (ids, formMap) =>
          pages.filter(_.formId.inSet(ids)).result.flatMap(pagesList => {
            val ids = pagesList.map(_.pageId.get)

            ((pages returning (pages.map(_.formPageId))) ++= pagesList.map(p => p.copy(pageId = None, formId = formMap(p.formId))))
              .map(res => ids.zip(res).toMap)
              .map(res => (ids, res))
          })
        }
        .flatMap { case (ids, pageMap) =>
          fields.filter(_.pageId.inSet(ids)).result.flatMap(fieldsList => {
            val ids = fieldsList.map(_.fieldId.get)

            ((fields returning (fields.map(_.fieldId))) ++= fieldsList.map(f => f.copy(fieldId = None, pageId = pageMap(f.pageId))))
              .map(res => ids.zip(res).toMap)
              .map(res => (ids, res))
          })
        }
        .flatMap { case (ids, fieldsMap) =>
          val base = fieldsAdditional.map(fa => (fa.fieldId, fa.key, fa.value))

          base.filter(_._1.inSet(ids)).result.flatMap(faList => {
            base ++= faList.map(tr => (fieldsMap(tr._1), tr._2, tr._3))
          })
        }
    )

}
