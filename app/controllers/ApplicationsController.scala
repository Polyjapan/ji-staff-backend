package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.FormModel
import models.FormModel.FormModel
import play.api.libs.json._
import play.api.mvc._
import services.AuthParserService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ApplicationsController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, auth: AuthParserService, model: FormModel)(implicit exec: ExecutionContext) extends AbstractController(cc) {
/*
# update your application for an edition
PUT     /applications/:year         controllers.ApplicationsController.updateApplication(year: String)
# get your application for an edition
GET     /applications/:year         controllers.ApplicationsController.getApplication(year: String)
# get all sent applications with user profile for an edition (comitee only)
GET     /applications/sent/:year    controllers.ApplicationsController.getSentApplications(year: String)
# accept or refuse an application (comitee only)
PUT     /applications/:userId/:year controllers.ApplicationsController.setAccepted(userId: String, year: String)
 */
  def updateApplication(year: String) = TODO
  def getApplication(year: String) = TODO
  def getSentApplications(year: String) = TODO
  def setAccepted(userId: String, year: String) = TODO
}
