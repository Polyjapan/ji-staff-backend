package controllers.backoffice

import ch.japanimpact.auth.api.AuthApi
import data.Meals.Meal
import javax.inject.Inject
import models.MealsModel
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class MealsController @Inject()(cc: ControllerComponents, auth: AuthApi, meals: MealsModel)
                               (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  def createMeal: Action[Meal] = Action.async(parse.json[Meal]) { rq =>
    meals.createMeal(rq.body).map(r => Ok(Json.toJson(r)))
  }

  def getMeals(event: Int) = Action.async {
    meals.getMeals(event).map(r => Ok(Json.toJson(r)))
  }

  def getMeal(meal: Int) = Action.async {
    meals.getMeal(meal).map {
      case Some(r) => Ok(Json.toJson(r))
      case None => NotFound
    }
  }

  def getMealTaken(meal: Int) = Action.async {
    meals.getMealsTaken(meal).map(r => Ok(Json.toJson(r)))
  }

  def takeMeal(meal: Int): Action[Int] = Action.async(parse.json[Int]) { rq =>
    meals.takeMeal(meal, rq.body).map(r => Ok(Json.toJson(r)))
  }
}
