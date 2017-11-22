package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.EditionsModel
import play.api.mvc._
import services.AuthParserService
import tools.{FutureMappers, TemporaryEdition}

import scala.concurrent.ExecutionContext

@Singleton
class OptionsController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) with FutureMappers {
  def headers = List(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
    "Access-Control-Max-Age" -> "3600",
    "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept, Authorization",
    "Access-Control-Allow-Credentials" -> "true"
  )
  def rootOptions = options("/")

  def options(url: String) = Action { request =>
    NoContent.withHeaders(headers : _*)
  }
}
