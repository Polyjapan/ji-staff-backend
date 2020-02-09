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
    db.run(
      meals.filter(_.mealId === meal)
        .joinLeft(mealsTaken).on { case (meal, mealTaken) => meal.mealId === mealTaken.mealId && mealTaken.userId === user }
        .result.headOption
    )
      .flatMap {
        case Some((meal, None)) =>
          db.run(mealsTaken += MealTaken(meal.mealId.get, user, Some(new Timestamp(System.currentTimeMillis()))))
          .map(_ => (true, meal.eventId))
        case Some((meal, _)) => Future.successful((false, meal.eventId))
      }
      .flatMap {
        case (success, eventId) =>
          getFoodParticularities(eventId, user).map(foodPart => (success, foodPart))
      }
  }

  def getFoodParticularities(event: Int, user: Int) = {
    db.run {
      val admin = adminFoodParticularities.filter(_.userId === user).map(_.foodParticularities).result.headOption
      val staff = staffFoodParticularities.filter(_.eventId === event)
        .join(applications).on { case (_, app) => app.userId === user }
        .join(applicationsContents).on { case ((part, app), appContent) => appContent.applicationId === app.applicationId && appContent.fieldId === part.particularitiesField }
        .map { case (_, content) => content.value }
        .result.headOption

      admin flatMap(adminFood => staff.map(staffFood => adminFood.orElse(staffFood).getOrElse("N/A")))
    }
  }

  def getStaffFoodParticularities(event: Int) =
    db.run(staffFoodParticularities.filter(_.eventId === event).map(_.particularitiesField).result.headOption)

  def getAdminFoodParticularities =
    db.run(adminFoodParticularities.result).map(_.toMap)

  def setStaffFoodParticularities(event: Int, field: Int) =
    db.run(staffFoodParticularities.insertOrUpdate((event, field)))

  def setAdminFoodParticularities(particularities: List[(Int, String)]) =
    db.run(DBIO.sequence(particularities.map(pair => adminFoodParticularities.insertOrUpdate(pair)))).map(_.sum)

}
