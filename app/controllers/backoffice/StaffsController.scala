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

  case class StaffLine(staffNumber: Int, applicationId: Int, user: UserProfile)

  implicit val staffLineWrites: Writes[StaffLine] = Json.writes[StaffLine]

  def getStaffs(event: Int): Action[AnyContent] = Action.async({
    staffs.listStaffs(event).flatMap(list => {
      val staffIdMap = list.map(line => (line.user.userId, (line))).toMap

      auth.getUserProfiles(staffIdMap.keySet).map {
        case Left(seq) => Ok(Json.toJson(seq.map {
          case (k, v) =>
            val staff = staffIdMap(k)
            StaffLine(staff.staffNumber, staff.application, v)
        }))
        case Right(_) => InternalServerError
      }
    })
  }).requiresAuthentication

  def exportStaffs(event: Int): Action[AnyContent] = Action.async({
    staffs.listStaffsDetails(event).flatMap {
      case (fields, map) =>
        val userIds = map.map(_._1._2).toSet
        val fieldsOrdering = fields.map(_.fieldId.get).zipWithIndex.toMap

        auth.getUserProfiles(userIds).map {
          case Left(profiles) =>

            val header =
              List("#", "Prénom", "Nom", "Téléphone", "Email", "Adresse", "Adresse 2", "NPA", "Ville", "Pays") ++ fields.map(_.name)

            val lines: Seq[Seq[String]] = header :: map.toList.sortBy(_._1._1).map {
              case ((staffId, userId), content) =>
                val profile = profiles(userId)
                val orderedContent = content.toList.sortBy(pair => fieldsOrdering(pair._1))

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
