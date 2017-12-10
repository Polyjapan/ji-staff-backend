package data

import play.api.libs.json.{Json, OFormat}

/**
  * A class representing a comment sent by an admin
  * @author zyuiop
  */
case class Comment(authorName: String, authorId: String, date: Long, comment: String)

object Comment {
  /**
    * The formatter used by the play framework Json library to convert json to/from comments
    */
  implicit val format: OFormat[Comment] = Json.format[Comment]
}