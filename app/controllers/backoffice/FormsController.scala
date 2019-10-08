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

  def getForms(event: Int): Action[AnyContent] = Action.async(forms.getForms(event).map(r => Ok(Json.toJson(r)))).requiresAuthentication

  def createForm: Action[Form] = Action.async(parse.json[Form])(rq =>
    forms.createForm(rq.body.copy(formId = None)).map(id => Ok(Json.toJson(id)))
  ).requiresAuthentication

  def updateForm(form: Int): Action[Form] = Action.async(parse.json[Form])(rq =>
    forms.updateForm(rq.body.copy(formId = Some(form))).map(res => if (res > 0) Ok else NotFound)
  ).requiresAuthentication

  def createPage(form: Int): Action[FormPage] = Action.async(parse.json[FormPage])(rq =>
    forms.createPage(rq.body.copy(pageId = None, formId = form)).map(id => Ok(Json.toJson(id)))
  ).requiresAuthentication

  def updatePage(form: Int, page: Int): Action[FormPage] = Action.async(parse.json[FormPage])(rq =>
    forms.updatePage(rq.body.copy(pageId = Some(page), formId = form)).map(res => if (res > 0) Ok else NotFound)
  ).requiresAuthentication

  def createField(form: Int, page: Int): Action[Forms.Field] = Action.async(parse.json[Forms.Field])(rq =>
    forms.createField(rq.body.copy(fieldId = None, pageId = page)).map(id => Ok(Json.toJson(id)))
  ).requiresAuthentication

  def updateField(form: Int, page: Int, field: Int): Action[Forms.Field] = Action.async(parse.json[Forms.Field])(rq =>
    forms.updateField(rq.body.copy(fieldId = Some(field), pageId = page)).map(res => if (res > 0) Ok else NotFound)
  ).requiresAuthentication

  def setAdditional(form: Int, page: Int, field: Int, key: String): Action[String] = Action.async(parse.text(100))(rq =>
    forms.setAdditional(field, key, rq.body).map(res => Ok)
  ).requiresAuthentication

  def removeAdditional(form: Int, page: Int, field: Int, key: String): Action[AnyContent] = Action.async(rq =>
    forms.deleteAdditional(field, key).map(res => Ok)
  ).requiresAuthentication

  def getPageById(form: Int, page: Int): Action[AnyContent] = Action async forms.getPageById(form, page).map(forms.encodePage).map {
    case Some(json) => Ok(json)
    case _ => NotFound
  }
}
