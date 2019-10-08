package models

import data.Forms.{Field, Form}
import data._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
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

  def getForms(event: Int): Future[Seq[Form]] =
    db.run(forms.filter(_.eventId === event).result)

  def getPages(form: Int): Future[Seq[Forms.FormPage]] =
    db.run(pages.filter(_.formId === form).result).map(_.sorted)

  def getPage(form: Int, page: Int): Future[Option[(Forms.FormPage, List[(Field, Map[String, String])])]] = {
    getPages(form).flatMap(pages => {
      if (pages.length >= page && page > 0) {
        val pg = pages(page - 1)

        getPageContent(pg).map(list => Some(pg, list))
      } else Future(None)
    })
  }

  private def getPageContent(formPage: Forms.FormPage): Future[List[(Field, Map[String, String])]] = {
    db.run(
      fields.filter(_.pageId === formPage.pageId)
        .joinLeft(fieldsAdditional).on(_.fieldId === _.fieldId)
        .result
    )
      .map(list => list.groupBy(_._1).mapValues(_.flatMap(_._2).toMap).toList)
  }

  def getPageById(form: Int, pageId: Int): Future[Option[(Forms.FormPage, List[(Field, Map[String, String])])]] = {
    db.run(pages.filter(pg => pg.formId === form && pg.formPageId === pageId).result.headOption)
      .flatMap {
        case Some(pg) => getPageContent(pg).map(content => Some((pg, content)))
        case None => Future.successful(None)
      }
  }

  def encodePage(pg: Option[(Forms.FormPage, List[(Field, Map[String, String])])]) = {
    pg.map {
      case (page, fields) => Json.obj(
        "page" -> page,
        "fields" -> fields.sortBy(_._1).map {
          case (field, map) => Json.obj("field" -> field, "additional" -> map)
        }
      )
    }
  }

  def cloneEvent(source: Int, target: Int): Future[_] =
  // List forms
    db.run(
      forms.filter(_.eventId === source).result.flatMap[(Seq[Int], Map[Int, Int]), NoStream, Effect.All](
        formsList => {
          val ids = formsList.map(_.formId.get)

          ((forms returning (forms.map(_.formId))) ++= formsList.map(f => f.copy(formId = None, eventId = target)))
            .map(res => ids.zip(res).toMap)
            .map(res => (ids, res))
        })
        .flatMap[(Seq[Int], Map[Int, Int]), NoStream, Effect.All] { case (ids, formMap) =>
          pages.filter(_.formId.inSet(ids)).result.flatMap(pagesList => {
            val ids = pagesList.map(_.pageId.get)

            ((pages returning (pages.map(_.formPageId))) ++= pagesList.map(p => p.copy(pageId = None, formId = formMap(p.formId))))
              .map(res => ids.zip(res).toMap)
              .map(res => (ids, res))
          })
        }
        .flatMap[(Seq[Int], Map[Int, Int]), NoStream, Effect.All] { case (ids, pageMap) =>
          fields.filter(_.pageId.inSet(ids)).result.flatMap(fieldsList => {
            val ids = fieldsList.map(_.fieldId.get)

            ((fields returning (fields.map(_.fieldId))) ++= fieldsList.map(f => f.copy(fieldId = None, pageId = pageMap(f.pageId))))
              .map(res => ids.zip(res).toMap)
              .map(res => (ids, res))
          })
        }
        .flatMap[Option[Int], NoStream, Effect.All] { case (ids, fieldsMap) =>
          val base = fieldsAdditional.map(fa => (fa.fieldId, fa.key, fa.value))

          base.filter(_._1.inSet(ids)).result.flatMap(faList => {
            base ++= faList.map(tr => (fieldsMap(tr._1), tr._2, tr._3))
          })
        }
    )

  def createForm(form: Form): Future[Int] =
    db.run(forms returning (forms.map(_.formId)) += form)

  def updateForm(form: Form): Future[Int] =
    db.run(forms.filter(f => f.formId === form.formId.get && f.eventId === form.eventId).update(form))

  def createPage(page: Forms.FormPage): Future[Int] =
    db.run(pages.returning(pages.map(_.formPageId)) += page)

  def updatePage(page: Forms.FormPage): Future[Int] =
    db.run(pages.filter(p => p.formPageId === page.pageId.get && p.formId === page.formId).update(page))

  def createField(field: Forms.Field): Future[Int] =
    db.run(fields.returning(fields.map(_.fieldId)) += field)

  def updateField(field: Forms.Field): Future[Int] =
    db.run(fields.filter(f => f.fieldId === field.fieldId.get && f.pageId === field.pageId).update(field))

  def deleteField(form: Int, page: Int, field: Int): Future[Int] =
    db.run(fields.filter(f => f.fieldId === field && f.pageId === page).delete)

  def deletePage(form: Int, id: Int): Future[Int] =
    db.run(pages.filter(pg => pg.formId === form && pg.formPageId === id).delete)

  def deleteForm(form: Int): Future[Int] =
    db.run(forms.filter(f => f.formId === form).delete)

  def setAdditional(field: Int, key: String, value: String): Future[Int] = {
    db.run(fieldsAdditional.map(fa => (fa.fieldId, fa.key, fa.value))
      .insertOrUpdate((field, key, value)))
  }

  def deleteAdditional(field: Int, key: String): Future[Int] = {
    db.run(fieldsAdditional.filter(fa => fa.fieldId === field && fa.key === key).delete)
  }

}
