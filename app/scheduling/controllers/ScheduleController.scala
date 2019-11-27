package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}
import scheduling.SchedulingService
import scheduling.models.SchedulingModel

import scala.concurrent.ExecutionContext
import utils.AuthenticationPostfix._

@Singleton
class ScheduleController @Inject()(cc: ControllerComponents, model: SchedulingModel, service: SchedulingService)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getSchedule(project: Int) = Action.async(req => ???).requiresAuthentication

  def generateSchedule(project: Int) = Action.async(req => {
    service.buildSchedule(project).map(_ => Ok)
  }).requiresAuthentication
}
