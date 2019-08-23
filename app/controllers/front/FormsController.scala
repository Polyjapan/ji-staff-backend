package controllers.front

import javax.inject.{Inject, Singleton}
import models.FormsModel
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class FormsController @Inject()(cc: ControllerComponents, forms: FormsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getMainForm: Action[AnyContent] = Action async forms.getMainForm.map {
    case Some(r) => Ok(Json.toJson(r))
    case None => NotFound
  }

  def getForms: Action[AnyContent] = Action async forms.getForms.map(r => Ok(Json.toJson(r)))

  def getForm(form: Int): Action[AnyContent] = Action async forms.getForm(form).map {
    case Some(e) => Ok(Json.toJson(e))
    case None => NotFound
  }

  def getPages(form: Int): Action[AnyContent] = Action async forms.getPages(form).map {
    case seq if seq.nonEmpty => Ok(Json.toJson(seq))
    case _ => NotFound
  }

  def getPage(form: Int, page: Int): Action[AnyContent] = Action async forms.getPage(form, page).map {
    case Some((page, fields)) =>
      Ok(Json.obj(
        "page" -> page,
        "fields" -> fields.sortBy(_._1).map {
          case (field, map) => Json.obj("field" -> field, "additional" -> map)
        }
      ))
    case _ => NotFound
  }

}
