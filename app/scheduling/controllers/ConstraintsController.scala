package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}
import scheduling.models.SchedulingModel
import utils.AuthenticationPostfix._
import scala.concurrent.ExecutionContext

@Singleton
class ConstraintsController @Inject()(cc: ControllerComponents, model: SchedulingModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getConstraints(project: Int) = Action.async(req => {???}).requiresAuthentication

  def createConstraint(project: Int) = Action.async(req => ???).requiresAuthentication
}
