package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scheduling.{ScheduleColumn, ScheduleDay, ScheduleLine, SchedulingService, StaffData}
import scheduling.models.{SchedulingModel, TaskSlot}

import scala.concurrent.ExecutionContext
import utils.AuthenticationPostfix._

@Singleton
class ScheduleController @Inject()(cc: ControllerComponents, model: SchedulingModel, service: SchedulingService)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {


  implicit val staffDataFormat = Json.writes[StaffData]

  implicit val scheduleLineStaffFormat = Json.writes[ScheduleLine[StaffData]]
  implicit val scheduleLineTaskFormat = Json.writes[ScheduleLine[String]]

  implicit val scheduleColumnStaffFormat = Json.writes[ScheduleColumn[String, StaffData]]
  implicit val scheduleColumnTaskFormat = Json.writes[ScheduleColumn[StaffData, String]]

  implicit val scheduleDayStaffFormat = Json.writes[ScheduleDay[String, StaffData]]
  implicit val scheduleDayTaskFormat = Json.writes[ScheduleDay[StaffData, String]]

  // Map[Staff, List[TaskSlot with Task]]
  def getScheduleByStaff(project: Int) =
    Action.async(req => model.getScheduleByStaff(project).map(res => {
      Ok(Json.toJson(res.toList))
    }))//.requiresAuthentication

  // Map[Staff, List[TaskSlot with Task]]
  def getScheduleByStaffHtml(project: Int) =
    Action.async(req => model.getScheduleByStaff(project).map(res => {
      Ok(views.html.schedule.schedule(res.toList))
    }))//.requiresAuthentication

  // Map[Task, List[TaskSlot with Staff]]
  def getScheduleByTask(project: Int) =
    Action.async(req => model.getScheduleByTasks(project).map(res => {
      println(Json.toJson(res.head.schedule.head.content.head))
      Ok(Json.toJson(res.toList))
    }))//.requiresAuthentication

  // Map[Task, List[TaskSlot with Staff]]
  def getScheduleByTaskHtml(project: Int) =
    Action.async(req => model.getScheduleByTasks(project).map(res => {
      // TODO: fix template!
      Ok(views.html.schedule.scheduleByTask(res.toList))
    }))//.requiresAuthentication


  def generateSchedule(project: Int) = Action.async(req => {
    service.buildSchedule(project).map(res => Ok(Json.toJson(res)))
  })
}
