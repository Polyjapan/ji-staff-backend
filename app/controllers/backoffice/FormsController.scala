package controllers.backoffice

import java.sql.Timestamp

import data.AuthenticationPostfix._
import data.Forms.Form
import javax.inject.{Inject, Singleton}
import models.FormsModel
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
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

}
