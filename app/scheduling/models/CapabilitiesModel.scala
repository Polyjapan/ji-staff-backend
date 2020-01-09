package scheduling.models

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

class CapabilitiesModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getCapabilities: Future[Seq[(Int, String)]] = {
    db.run(capabilities.result)
  }

  def createCapability(value: String): Future[Int] = {
    db.run(capabilities.map(_.name).returning(capabilities.map(_.id)) += value)
  }

  def resolveCapabilitiesMap(values: List[String]): Future[Map[String, Int]] = {
    // Find the ones we can
    val valueSet = values.toSet
    db.run(capabilities.filter(cap => cap.name.inSet(valueSet)).result)
      .map(seq => seq.map { case (k, v) => (v, k) }.toMap)
      .flatMap { map => {
        val missingValues = valueSet.filter(k => !map.contains(k))

        if (missingValues.isEmpty) Future(map)
        else {
          val insertList = missingValues.toList
          db.run(capabilities.map(_.name).returning(capabilities.map(_.id)) ++= insertList)
            .map(insertedIds => (insertList zip insertedIds).toMap)
            .map(insertedMap => map ++ insertedMap)
        }
      }}
  }

  def resolveCapabilitiesIds(values: List[String]): Future[Set[Int]] = {
    // Find the ones we can
    val valueSet = values.toSet
    db.run(capabilities.filter(cap => cap.name.inSet(valueSet)).result)
      .map(seq => seq.map { case (k, v) => (v, k) }.toMap)
      .flatMap { map => {
        val missingValues = valueSet.filter(k => !map.contains(k))
        val goodValues = map.values.toSet

        if (missingValues.isEmpty) Future(goodValues)
        else {
          db.run(capabilities.map(_.name).returning(capabilities.map(_.id)) ++= missingValues)
            .map(inserted => goodValues ++ inserted)
        }
      }}
  }
}
