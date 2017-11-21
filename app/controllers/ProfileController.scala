package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.FormModel.FormModel
import models.ProfileModel.ProfileModel
import play.api.mvc._
import services.AuthParserService

import scala.concurrent.ExecutionContext

@Singleton
class ProfileController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, formModel: FormModel, profileModel: ProfileModel)(implicit exec: ExecutionContext) extends AbstractController(cc) {
  def getProfile = play.mvc.Results.TODO
  def setProfile = play.mvc.Results.TODO
}
