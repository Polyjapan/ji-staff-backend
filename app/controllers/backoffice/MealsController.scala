package controllers.backoffice

import data.Meals.Meal
import javax.inject.Inject
import models.MealsModel
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class MealsController @Inject()(cc: ControllerComponents, meals: MealsModel)
                               (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  def createMeal: Action[Meal] = Action.async(parse.json[Meal]) { rq =>
    meals.createMeal(rq.body).map(r => Ok(Json.toJson(r)))
  }.requiresAdmin

  def getMeals(event: Int) = Action.async {
    meals.getMeals(event).map(r => Ok(Json.toJson(r)))
  }.requiresAdmin

  def getMeal(meal: Int) = Action.async {
    meals.getMeal(meal).map {
      case Some(r) => Ok(Json.toJson(r))
      case None => NotFound
    }
  }.requiresAdmin

  def getMealTaken(meal: Int) = Action.async {
    meals.getMealsTaken(meal).map(r => Ok(Json.toJson(r)))
  }.requiresAdmin

  def takeMeal(meal: Int): Action[Int] = Action.async(parse.json[Int]) { rq =>
    meals.takeMeal(meal, rq.body).map(r => Ok(Json.obj("success" -> r._1, "foodParticularities" -> r._2)))
  }.requiresAdmin

  def getStaffFoodParticularties(event: Int) = Action.async {
    meals.getStaffFoodParticularities(event).map(res => Ok(Json.toJson(res)))
  }.requiresAdmin

  def setStaffFoodParticularities(event: Int): Action[Int] = Action.async(parse.json[Int]) { rq =>
    meals.setStaffFoodParticularities(event, rq.body).map(res => Ok)
  }.requiresAdmin

  def getAdminFoodParticularties() = Action.async {
    meals.getAdminFoodParticularities.map(res => Ok(Json.toJson(res.map { case (k, v) => (k.toString, v) })))
  }.requiresAdmin

  def setAdminFoodParticularities(): Action[Map[String, String]] = Action.async(parse.json[Map[String, String]]) { rq =>
    meals.setAdminFoodParticularities(rq.body.toList.map { case (k, v) => (k.toInt, v) }).map(res => Ok)
  }.requiresAdmin
}
