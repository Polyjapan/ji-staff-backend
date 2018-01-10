package services

import java.nio.ByteBuffer
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Paths}
import java.util.{Base64, UUID}
import javax.inject.{Inject, Singleton}

import play.api.Configuration
import play.api.libs.Files.TemporaryFile
import collection.JavaConverters._


/**
  * @author zyuiop
  */
object UploadsService {
  type MimeType = (String, String)

  val images: List[MimeType] = List(
    ("image/jpeg", ".jpg"),
    ("image/bmp", ".bmp"),
    ("image/png", ".png"),
    ("image/tiff", ".tiff")
  )

  val pdf: MimeType = ("application/pdf", ".pdf")
}

@Singleton
class UploadsService @Inject()(config: Configuration) {

  /**
    * Finishes uploading a file. This method checks the mime type of the file and moves it to the uploads path with a
    * random name
    * @param file the file being uploaded
    * @param allowedTypes a list of mime types that are accepted for this upload
    * @return a pair (success, new file name)
    */
  def upload(file: TemporaryFile, allowedTypes: List[UploadsService.MimeType], name: => String = randomId): (Boolean, String) = {
    val t = Files.probeContentType(file.path)

    val matching = allowedTypes.filter(_._1 == t)

    if (matching.isEmpty) {
      file.delete()
      (false, "")
    } else {
      val fileName = name + matching.head._2
      val path = config.get[String]("uploads.path") + fileName
      file.moveTo(Paths.get(path), replace = true)
      Files.setPosixFilePermissions(Paths.get(path), Set(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OTHERS_READ, PosixFilePermission.GROUP_READ).asJava)
      println("file saved at " + path)
      (true, fileName)
    }
  }

  private def randomId: String = {
    // Create random UUID
    val uuid = UUID.randomUUID
    // Create byte[] for base64 from uuid
    val src = ByteBuffer.wrap(new Array[Byte](16)).putLong(uuid.getMostSignificantBits).putLong(uuid.getLeastSignificantBits).array
    // Encode to Base64 and remove trailing ==
    Base64.getUrlEncoder.encodeToString(src).substring(0, 22)
  }
}
