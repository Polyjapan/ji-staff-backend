package controllers.front

import data.User
import javax.inject.{Inject, Singleton}
import models.AppsModel._
import models.{AppsModel, StaffsModel, UsersModel}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class StaffsController @Inject()(cc: ControllerComponents)(implicit apps: AppsModel, staffs: StaffsModel, ec: ExecutionContext) extends AbstractController(cc) {
  def isStaff(user: Int): Action[AnyContent] = Action.async(
    staffs.getStaffIdForCurrentEvent(user).map(resp => Ok(Json.obj("is_staff" -> resp.isDefined)))
  ).requiresApp

  def getStaffNumber(user: Int): Action[AnyContent] = Action.async(staffs.getStaffIdForCurrentEvent(user).map {
    case Some(staffId) => Ok(Json.obj("staff_id" -> staffId))
    case _ => NotFound
  }).requiresApp

  def getStaffList = Action.async(staffs.listStaffsForCurrentEvent.map {
    case lst => Ok(Json.toJson(lst.map(staff => List(staff.staffNumber, staff.user.userId))))
  }).requiresApp
}
