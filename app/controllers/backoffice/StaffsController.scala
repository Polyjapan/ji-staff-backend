package controllers.backoffice

import java.io.ByteArrayOutputStream

import ch.japanimpact.auth.api.{AuthApi, UserAddress, UserProfile}
import javax.inject.Inject
import models.StaffsModel
import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.AuthenticationPostfix._

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class StaffsController @Inject()(cc: ControllerComponents, auth: AuthApi, staffs: StaffsModel)
                                (implicit ec: ExecutionContext, conf: Configuration) extends AbstractController(cc) {

  case class StaffLine(staffNumber: Int, applicationId: Int, user: UserProfile, level: Int, capabilities: List[String])

  implicit val staffLineWrites: Writes[StaffLine] = Json.writes[StaffLine]

  def getStaffs(event: Int): Action[AnyContent] = Action.async({
    staffs.listStaffs(event).flatMap(list => {
      val staffIdMap = list.map(line => (line.user.userId, (line))).toMap

      auth.getUserProfiles(staffIdMap.keySet).map {
        case Left(seq) => Ok(Json.toJson(seq.map({
          case (k, v) =>
            val staff = staffIdMap(k)
            StaffLine(staff.staffNumber, staff.application, v, staff.level, staff.capabilities)
        }).toList.sortBy(l => l.staffNumber)  ))
        case Right(_) => InternalServerError
      }
    })
  }).requiresAuthentication

  def setLevels(event: Int): Action[List[(Int, Int)]] = Action.async(parse.json[List[(Int, Int)]]) { req =>
    staffs.setLevels(event, req.body).map(r => Ok)
  }.requiresAuthentication

  def addCapabilities(event: Int): Action[List[List[Int]]] = Action.async(parse.json[List[List[Int]]]) { req =>
    val caps =
      req.body.filter(_.size > 1)
          .map(list => (list.head, list.tail))
          .flatMap { case (staffId, caps) => caps.map(cap => (event, staffId, cap) )}

    staffs.addCapabilities(caps).map(_ => Ok)
  }.requiresAuthentication

  def exportStaffs(event: Int): Action[AnyContent] = Action.async({
    staffs.listStaffsDetails(event).flatMap {
      case (fields, map) =>
        val userIds = map.map(_._1._2).toSet
        val fieldsOrdering = fields.map(_.fieldId.get).zipWithIndex.toMap

        auth.getUserProfiles(userIds).map {
          case Left(profiles) =>

            val header =
              List("#", "Prénom", "Nom", "Téléphone", "Email", "Adresse", "Adresse 2", "NPA", "Ville", "Pays", "Date de naissance") ++ fields.map(_.name)

            val lines: Seq[Seq[String]] = header :: map.toList.sortBy(_._1._1).map {
              case ((staffId, userId, birthdate), content) =>
                val profile = profiles(userId)
                val missingIds = fieldsOrdering.keySet -- content.map(_._1).toSet

                val orderedContent = (content.toList ++ missingIds.map(id => (id, ""))).sortBy(pair => fieldsOrdering(pair._1))

                val address = profile.address.getOrElse(UserAddress("Unknown", None, "Unknown", "Unknown", "Unknown"))

                List(
                  staffId.toString,
                  profile.details.firstName,
                  profile.details.lastName,
                  profile.details.phoneNumber.getOrElse(""),
                  profile.email,
                  address.address,
                  address.addressComplement.getOrElse(""),
                  address.postCode,
                  address.city,
                  address.country,
                  birthdate.toString
                ) ::: orderedContent.map(_._2)
            }

            import com.github.tototoshi.csv._
            val os = new ByteArrayOutputStream()
            val csv = CSVWriter.open(os, "UTF-8")
            csv.writeAll(lines)
            csv.close()

            Ok(os.toString("UTF-8")).as("text/csv; charset=utf-8")

          case Right(_) => InternalServerError
        }
    }
  }).requiresAuthentication


}
