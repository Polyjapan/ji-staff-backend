package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scheduling.models.{ConstraintsModel, SchedulingModel}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

@Singleton
class ConstraintsController @Inject()(cc: ControllerComponents, model: ConstraintsModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getConstraints(project: Int) = Action.async(req =>
    model.getConstraints(project).map(res => Ok(Json.toJson(res))))//.requiresAuthentication

  def createConstraint(project: Int) = Action.async(req => ???).requiresAuthentication
}
