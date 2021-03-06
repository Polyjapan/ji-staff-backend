package controllers.backoffice

import utils.AuthenticationPostfix._
import data.Forms
import data.Forms.{Form, FormPage}
import javax.inject.{Inject, Singleton}
import models.FormsModel
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class FormsController @Inject()(cc: ControllerComponents, forms: FormsModel)(implicit ec: ExecutionContext, config: Configuration) extends AbstractController(cc) {

  def getForms(event: Int): Action[AnyContent] = Action.async(forms.getForms(event).map(r => Ok(Json.toJson(r)))).requiresAdmin

  def createForm: Action[Form] = Action.async(parse.json[Form])(rq =>
    forms.createForm(rq.body.copy(formId = None, isMain = false)).map(id => Ok(Json.toJson(id)))
  ).requiresAdmin

  def updateForm(form: Int): Action[Form] = Action.async(parse.json[Form])(rq =>
    forms.updateForm(rq.body.copy(formId = Some(form))).map(res => if (res > 0) Ok else NotFound)
  ).requiresAdmin

  def createPage(form: Int): Action[FormPage] = Action.async(parse.json[FormPage])(rq =>
    forms.createPage(rq.body.copy(pageId = None, formId = form)).map(id => Ok(Json.toJson(id)))
  ).requiresAdmin

  def updatePage(form: Int, page: Int): Action[FormPage] = Action.async(parse.json[FormPage])(rq =>
    forms.updatePage(rq.body.copy(pageId = Some(page), formId = form)).map(res => if (res > 0) Ok else NotFound)
  ).requiresAdmin

  def createField(form: Int, page: Int): Action[Forms.Field] = Action.async(parse.json[Forms.Field])(rq =>
    forms.createField(rq.body.copy(fieldId = None, pageId = page)).map(id => Ok(Json.toJson(id)))
  ).requiresAdmin

  def updateField(form: Int, page: Int, field: Int): Action[Forms.Field] = Action.async(parse.json[Forms.Field])(rq =>
    forms.updateField(rq.body.copy(fieldId = Some(field), pageId = page)).map(res => if (res > 0) Ok else NotFound)
  ).requiresAdmin

  def deleteField(form: Int, page: Int, field: Int): Action[AnyContent] = Action.async(rq =>
    forms.deleteField(form, page, field).map(res => if (res > 0) Ok else NotFound)
  ).requiresAdmin

  def setAdditional(form: Int, page: Int, field: Int): Action[(String, Int)] = Action.async(parse.json[(String, Int)])(rq =>
    forms.setAdditional(field, if (rq.body._1.length > 100) rq.body._1.substring(0, 100) else rq.body._1, rq.body._2).map(res => Ok)
  ).requiresAdmin

  def removeAdditional(form: Int, page: Int, field: Int): Action[String] = Action.async(parse.text(100))(rq =>
    forms.deleteAdditional(field, rq.body).map(res => Ok)
  ).requiresAdmin

  def getPageById(form: Int, page: Int): Action[AnyContent] = Action async forms.getPageById(form, page).map(forms.encodePage).map {
    case Some(json) => Ok(json)
    case _ => NotFound
  }

  def deletePage(form: Int, id: Int): Action[AnyContent] = Action.async(
    forms.deletePage(form, id).map(res => if (res > 0) Ok else NotFound)
  ).requiresAdmin

  def deleteForm(form: Int): Action[AnyContent] = Action.async(
    forms.deleteForm(form).map(res => if (res > 0) Ok else NotFound)
  ).requiresAdmin


}
