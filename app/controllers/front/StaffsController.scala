package controllers.front

import ch.japanimpact.auth.api.apitokens
import ch.japanimpact.auth.api.apitokens.AuthorizationActions

import javax.inject.{Inject, Singleton}
import models.StaffsModel
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
@Singleton
class StaffsController @Inject()(cc: ControllerComponents)(implicit staffs: StaffsModel, authorize: AuthorizationActions, ec: ExecutionContext) extends AbstractController(cc) {
  def isStaff(user: Int): Action[AnyContent] = authorize("staff/list/user/is_staff").async(
    staffs.getStaffIdForCurrentEvent(user).map(resp => Ok(Json.obj("is_staff" -> resp.isDefined)))
  )

  def getStaffNumber(user: Int): Action[AnyContent] = authorize({
    case _: App => true
    case apitokens.User(id) => id == user
  }, _ match {
    case _: App => Set("staff/list/user/get_id")
    case _ => Set()
  }).async(staffs.getStaffIdForCurrentEvent(user).map {
    case Some(staffId) => Ok(Json.obj("staff_id" -> staffId))
    case _ => NotFound
  })

  def getStaffList: Action[AnyContent] = authorize("staff/list/event/current").async {
    staffs.listStaffsForCurrentEvent
      .map(lst => Ok(Json.toJson(lst.map(staff => List(staff.staffNumber, staff.user.userId)))))
  }

  def getStaffListForEvent(event: Int): Action[AnyContent] = authorize(s"staff/list/event/$event").async {
    staffs.listStaffs(event)
      .map(lst => Ok(Json.toJson(lst.map(staff => List(staff.staffNumber, staff.user.userId)))))
  }
}
