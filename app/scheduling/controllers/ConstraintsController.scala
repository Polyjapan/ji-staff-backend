package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import scheduling.constraints.ScheduleConstraint
import scheduling.models.{ConstraintsModel, SchedulingModel, TaskTimePartition}
import utils.AuthenticationPostfix._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConstraintsController @Inject()(cc: ControllerComponents, model: ConstraintsModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getConstraints(project: Int) = Action.async(req =>
    model.getConstraints(project).map(res => Ok(Json.toJson(res))))
    .requiresAdmin

  def createConstraint(project: Int): Action[ScheduleConstraint] = Action.async(parse.json[ScheduleConstraint])(req => {
    model.createConstraint(project, req.body).map(res => Ok(Json.toJson(res)))
  }).requiresAdmin

  def updateConstraint(project: Int, constraint: Int): Action[ScheduleConstraint] = Action.async(parse.json[ScheduleConstraint])(req => {
    model.updateConstraint(project, constraint, req.body).map(res => Ok(Json.toJson(res)))
  }).requiresAdmin

  def deleteConstraint(project: Int): Action[ScheduleConstraint] = Action.async(parse.json[ScheduleConstraint])(req => {
    model.deleteConstraint(project, req.body).map(res => Ok(Json.toJson(res)))
  }).requiresAdmin
}
