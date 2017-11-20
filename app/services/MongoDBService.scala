package services

import javax.inject.{Inject, Singleton}

import migrations.Migrations
import org.mongodb.scala.{MongoClient, MongoDatabase}
import play.api.Configuration

/**
  * @author Louis Vialar
  */
@Singleton
class MongoDBService @Inject()(configuration: Configuration, migrations: Migrations) {
  // The URI contains the credentials
  // This is the recommended approach for this driver - see https://mongodb.github.io/mongo-scala-driver/2.1/reference/connecting/authenticating/
  private val uri = s"mongodb://" +
    s"${configuration.get("mongodb.username")}:${configuration.get("mongodb.password")}" +
    s"@${configuration.get("mongodb.hostname")}:${configuration.get("mongodb.port")}/${configuration.get("mongodb.database")}"
  private lazy val client: MongoClient = MongoClient(uri)

  val database: MongoDatabase = client.getDatabase(configuration.get("mongodb.database"))

  migrations.migrate(database)
}
