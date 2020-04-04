package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, Action, ControllerComponents, Result}
import scheduling.models.{CapabilitiesModel, PartitionsModel, SchedulingModel, TasksModel, tasks}
import utils.AuthenticationPostfix._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TasksController @Inject()(cc: ControllerComponents, model: SchedulingModel, tasks: TasksModel, caps: CapabilitiesModel, partitions: PartitionsModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getTasks(project: Int) = Action.async(req => {
    tasks.getTasks(project).map(res => Ok(Json.toJson(res)))
  }).requiresAdmin

  def getTask(project: Int, task: Int) = Action.async(req => {
    tasks.getTask(project, task).map {
      case Some(res) => Ok(Json.toJson(res))
      case None => NotFound
    }
  }).requiresAdmin

  case class CreateUpdateTask(name: String, minAge: Option[Int], minExperience: Option[Int], taskType: Option[Int], addDifficulties: List[String], removeDifficulties: List[String])

  implicit val taskReads: Reads[CreateUpdateTask] = Json.reads[CreateUpdateTask]

  private def withResolvedCapabilities(task: CreateUpdateTask)(func: (Set[Int], Set[Int]) => Future[Result]) = {
    caps.resolveCapabilitiesIds(task.addDifficulties).flatMap(add => {
      caps.resolveCapabilitiesIds(task.removeDifficulties).flatMap(remove => func(add, remove))
    })
  }

  def createTask(project: Int): Action[CreateUpdateTask] = Action.async(parse.json[CreateUpdateTask])(req => {
    val task = scheduling.models.Task(None, project, req.body.name, req.body.minAge.getOrElse(0), req.body.minExperience.getOrElse(0), req.body.taskType)

    withResolvedCapabilities(req.body) { (add, remove) => tasks.createTask(task, add -- remove).map(r => Ok(Json.toJson(r)))}
  }).requiresAdmin

  def duplicateTask(project: Int): Action[Int] = Action.async(parse.json[Int])(req => {
    val copyOf = req.body
    tasks.getTask(project, copyOf).flatMap {
      case Some(task) =>
        caps.resolveCapabilitiesIds(task.difficulties).flatMap(caps => {
          tasks.createTask(scheduling.models.Task(None, task.projectId, task.name + " (copie)", task.minAge, task.minExperience, task.taskType), caps)
        }).flatMap(taskId => {
          partitions.getPartitionsForTask(project, copyOf)
            .map(parts => parts.map(partition => partition.copy(None, taskId)))
            .flatMap(parts => partitions.createPartitions(parts))
            .map(_ => Ok(Json.toJson(taskId)))
        })
      case None => Future(NotFound)
    }
  }).requiresAdmin

  def updateTask(project: Int, taskId: Int): Action[CreateUpdateTask] = Action.async(parse.json[CreateUpdateTask])(req => {
    val task = scheduling.models.Task(Some(taskId), project, req.body.name, req.body.minAge.getOrElse(0), req.body.minExperience.getOrElse(0), req.body.taskType)

    withResolvedCapabilities(req.body) { (add, remove) => tasks.updateTask(task, add, remove).map(r => Ok)}
  }).requiresAdmin

  def deleteTask(project: Int, taskId: Int) = Action.async(req => {
    tasks.deleteTask(project, taskId).map(_ => Ok)
  }).requiresAdmin

  def getTaskSlots(project: Int, task: Int) = Action.async(req => {
    tasks.getTaskSlots(project, task).map(r => Ok(Json.toJson(r)))
  }).requiresAdmin

  def generateSlots(project: Int, task: Int) = Action.async(body => {
    model.buildSlotsForTask(project, task).map {
      case true => Ok
      case false => NotFound
    }
  }).requiresAdmin
}
