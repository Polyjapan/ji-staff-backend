package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import scheduling.models.{PartitionsModel, SchedulingModel, TaskTimePartition}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

@Singleton
class PartitionsController @Inject()(cc: ControllerComponents, model: PartitionsModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {
  def getPartitionsForTask(project: Int, task: Int) = Action.async(req => {
    model.getPartitionsForTask(project, task)
      .map(res => Ok(Json.toJson(res)))
  }).requiresAdmin

  def createPartitionForTask(project: Int, task: Int): Action[TaskTimePartition] = Action.async(parse.json[TaskTimePartition])(req => {
    model.createPartition(req.body.copy(task = task)).map(r => Ok(Json.toJson(r)))
  }).requiresAdmin

  def updatePartition(project: Int, task: Int, partition: Int): Action[TaskTimePartition] = Action.async(parse.json[TaskTimePartition])(req => {
    model.updatePartition(req.body.copy(taskPartitionId = Some(partition), task = task)).map(r => Ok)
  }).requiresAdmin


  def deletePartition(project: Int, task: Int, partition: Int) = Action.async(req => {
    model.deletePartition(task, partition).map(r => Ok)
  }).requiresAdmin

}
