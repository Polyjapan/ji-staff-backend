package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import scheduling.models.{SchedulingModel, TasksModel}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

@Singleton
class TasksController @Inject()(cc: ControllerComponents, model: SchedulingModel, tasks: TasksModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getTasks(project: Int) = Action.async(req => {
    tasks.getTasks(project).map(res => Ok(Json.toJson(res)))
  }).requiresAuthentication

  def getTask(project: Int, task: Int) = Action.async(req => {
    tasks.getTask(project, task).map {
      case Some(res) => Ok(Json.toJson(res))
      case None => NotFound
    }
  }).requiresAuthentication

  case class CreateUpdateTask(name: String, minAge: Option[Int], minExperience: Option[Int], difficulties: List[Int])

  implicit val taskReads: Reads[CreateUpdateTask] = Json.reads[CreateUpdateTask]

  def createTask(project: Int): Action[CreateUpdateTask] = Action.async(parse.json[CreateUpdateTask])(req => {
    val task = scheduling.models.Task(None, project, req.body.name, req.body.minAge.getOrElse(0), req.body.minExperience.getOrElse(0))

    tasks.createTask(task, req.body.difficulties).map(r => Ok(Json.toJson(r)))
  }).requiresAuthentication

  def updateTask(project: Int, taskId: Int): Action[CreateUpdateTask] = Action.async(parse.json[CreateUpdateTask])(req => {
    val task = scheduling.models.Task(Some(taskId), project, req.body.name, req.body.minAge.getOrElse(0), req.body.minExperience.getOrElse(0))

    tasks.updateTask(task, req.body.difficulties).map(r => Ok)
  }).requiresAuthentication

  def getTaskSlots(project: Int, task: Int) = Action.async(req => {
    tasks.getTaskSlots(project, task).map(r => Ok(Json.toJson(r)))
  }).requiresAuthentication

  def generateSlots(project: Int, task: Int) = Action.async(body => {
    model.buildSlotsForTask(project, task).map {
      case true => Ok
      case false => NotFound
    }
  }).requiresAuthentication
}
