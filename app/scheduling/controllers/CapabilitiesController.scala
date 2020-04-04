package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scheduling.models.CapabilitiesModel
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

@Singleton
class CapabilitiesController @Inject()(cc: ControllerComponents, model: CapabilitiesModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getCapabilities = Action.async(req => {
    model.getCapabilities.map(seq => Ok(Json.toJson(seq.map { case (id, cap) => Json.obj("id" -> id, "cap" -> cap)})))
  }).requiresAdmin

  def createCapability = Action.async(parse.text(50))(req => {
    model.createCapability(req.body).map(res => Ok(Json.toJson(res)))
  }).requiresAdmin
}
