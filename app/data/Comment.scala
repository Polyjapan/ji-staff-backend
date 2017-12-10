package data

import play.api.libs.json.{Json, OFormat}

/**
  * @author zyuiop
  */
case class Comment(authorName: String, authorId: String, date: Long, comment: String)

object Comment {
  implicit val format: OFormat[Comment] = Json.format[Comment]
}