package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scheduling.models.{CapabilitiesModel, TaskTypesModel}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

@Singleton
class TaskTypesController @Inject()(cc: ControllerComponents, model: TaskTypesModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getTaskTypes = Action.async(req => {
    model.getTaskTypes.map(seq => Ok(Json.toJson(seq.map { case (id, tpe) => Json.obj("id" -> id, "type" -> tpe)})))
  }).requiresAuthentication

  def createTaskType = Action.async(parse.text(50))(req => {
    model.createTaskType(req.body).map(res => Ok(Json.toJson(res)))
  }).requiresAuthentication
}
