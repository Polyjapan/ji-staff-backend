package controllers.backoffice

import ch.japanimpact.auth.api.AuthApi
import data.Meals.Meal
import javax.inject.Inject
import models.MealsModel
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.ExecutionContext
import utils.AuthenticationPostfix._

/**
 * @author Louis Vialar
 */
class MealsController @Inject()(cc: ControllerComponents, auth: AuthApi, meals: MealsModel)
                               (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  def createMeal: Action[Meal] = Action.async(parse.json[Meal]) { rq =>
    meals.createMeal(rq.body).map(r => Ok(Json.toJson(r)))
  }.requiresAuthentication

  def getMeals(event: Int) = Action.async {
    meals.getMeals(event).map(r => Ok(Json.toJson(r)))
  }.requiresAuthentication

  def getMeal(meal: Int) = Action.async {
    meals.getMeal(meal).map {
      case Some(r) => Ok(Json.toJson(r))
      case None => NotFound
    }
  }.requiresAuthentication

  def getMealTaken(meal: Int) = Action.async {
    meals.getMealsTaken(meal).map(r => Ok(Json.toJson(r)))
  }.requiresAuthentication

  def takeMeal(meal: Int): Action[Int] = Action.async(parse.json[Int]) { rq =>
    meals.takeMeal(meal, rq.body).map(r => Ok(Json.obj("success" -> r._1, "foodParticularities" -> r._2)))
  }.requiresAuthentication

  def getStaffFoodParticularties(event: Int) = Action.async {
    meals.getStaffFoodParticularities(event).map(res => Ok(Json.toJson(res)))
  }.requiresAuthentication

  def setStaffFoodParticularities(event: Int): Action[Int] = Action.async(parse.json[Int]) { rq =>
    meals.setStaffFoodParticularities(event, rq.body).map(res => Ok)
  }.requiresAuthentication

  def getAdminFoodParticularties() = Action.async {
    meals.getAdminFoodParticularities.map(res => Ok(Json.toJson(res.map { case (k, v) => (k.toString, v) })))
  }.requiresAuthentication

  def setAdminFoodParticularities(): Action[Map[String, String]] = Action.async(parse.json[Map[String, String]]) { rq =>
    meals.setAdminFoodParticularities(rq.body.toList.map { case (k, v) => (k.toInt, v) }).map(res => Ok)
  }.requiresAuthentication
}
