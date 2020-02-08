package models

import java.sql.{Date, Timestamp}

import data.Meals.{Meal, MealTaken}
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class MealsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._

  def createMeal(meal: Meal): Future[Int] =
    db.run(meals returning (meals.map(_.mealId)) += meal.copy(date = Some(new Date(System.currentTimeMillis()))))

  def getMeals(eventId: Int): Future[Seq[Meal]] =
    db.run(meals.filter(_.eventId === eventId).result)

  def getMeal(mealId: Int): Future[Option[Meal]] =
    db.run(meals.filter(_.mealId === mealId).result.headOption)

  def getMealsTaken(meal: Int): Future[Seq[MealTaken]] =
    db.run(mealsTaken.filter(_.mealId === meal).result)

  def takeMeal(meal: Int, user: Int) = {
    db.run(mealsTaken.filter(m => m.mealId === meal && m.userId === user).result.headOption)
      .flatMap {
        case None => db.run(mealsTaken += MealTaken(meal, user, Some(new Timestamp(System.currentTimeMillis())))).map(_ => true)
        case Some(_) => Future.successful(false)
      }
  }

}
