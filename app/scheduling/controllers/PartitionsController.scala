package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}
import scheduling.models.{PartitionsModel, SchedulingModel}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

@Singleton
class PartitionsController @Inject()(cc: ControllerComponents, model: PartitionsModel)(implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  def getPartitions(project: Int) = Action.async(req => ???).requiresAuthentication

  def getPartitions(project: Int, task: Int) = Action.async(req => ???).requiresAuthentication

  def updatePartition(project: Int, partition: Int) = Action.async(req => ???).requiresAuthentication

  def getPartition(project: Int, partition: Int) = Action.async(req => ???).requiresAuthentication

  def createPartition(project: Int) = Action.async(req => ???).requiresAuthentication
}
