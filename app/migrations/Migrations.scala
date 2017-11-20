package migrations


import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.Indexes

import scala.util.Success

/**
  * @author Louis Vialar
  */
class Migrations {
  // method executed each time the server starts
  def migrate(database: MongoDatabase): Unit = {
    // Just run your migrations here !

    // 01 - create index on profiles
    database.createCollection("profiles").toFuture.andThen {
      // Collection created, let's add the index
      case _: Success[_] => database.getCollection("profiles").createIndex(Indexes.hashed("userId"))
    }

  }
}
