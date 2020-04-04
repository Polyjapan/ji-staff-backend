package scheduling.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scheduling.models.ProjectsModel

import scala.concurrent.{ExecutionContext, Future}
import utils.AuthenticationPostfix._

@Singleton
class ProjectsController @Inject()(cc: ControllerComponents, model: ProjectsModel)
                                  (implicit conf: Configuration, ec: ExecutionContext) extends AbstractController(cc) {

  case class CreateProject(name: String, maxHoursPerStaff: Int, minBreakMinutes: Int, maxSameShiftType: Int, copyOf: Option[Int], copySlots: Option[Boolean], copyConstraints: Option[Boolean])

  implicit val createProjectReads: Reads[CreateProject] = Json.reads[CreateProject]

  def getProjects(event: Int): Action[AnyContent] = Action.async(_ =>
    model.getProjects(event).map(lst => Ok(Json.toJson(lst)))
  ).requiresAdmin

  def getAllProjects: Action[AnyContent] = Action.async(_ =>
    model.getAllProjects.map(lst => Ok(Json.toJson(lst)))
  ).requiresAdmin

  def getProject(project: Int): Action[AnyContent] = Action.async(_ =>
    model.getProject(project).map {
      case Some(p) => Ok(Json.toJson(p))
      case None => NotFound
    }
  ).requiresAdmin

  def createProject(event: Int): Action[CreateProject] = Action.async(bodyParser = parse.json[CreateProject])(req => {
    model.createProject(event, req.body.name, req.body.maxHoursPerStaff, req.body.minBreakMinutes, req.body.maxSameShiftType)
      .flatMap(projectId => {
        if (req.body.copyOf.isDefined) {
          model.cloneProject(req.body.copyOf.get, projectId, req.body.copySlots.getOrElse(false), req.body.copyConstraints.getOrElse(false)).map(_ => projectId)
        } else Future.successful(projectId)
      })
      .map(id => Ok(Json.toJson(id)))
  }).requiresAdmin
}
