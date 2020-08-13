package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scheduling.models.{ProjectsModel, VersionsModel}
import utils.AuthenticationPostfix._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VersionsController @Inject()(cc: ControllerComponents, model: VersionsModel)
                                  (implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {


  def getVersions(project: Int): Action[AnyContent] = Action.async(_ =>
    model.getVersions(project).map(lst => Ok(Json.toJson(lst)))
  ).requiresAdmin

  def setActiveVersion(project: Int): Action[Int] = Action.async(
    parse.text(20)
      .validate(text => if (text.forall(_.isDigit)) Right(text.toInt) else Left(BadRequest))
  ) { rq =>
    val version = rq.body
    model.setActive(project, version).map(_ => Ok)
  }.requiresAdmin

  def setTag(project: Int, version: Int): Action[String] = Action.async(parse.text(200)) { rq =>
    model.setTag(project, version, Some(rq.body)).map(_ => Ok)
  }
}
