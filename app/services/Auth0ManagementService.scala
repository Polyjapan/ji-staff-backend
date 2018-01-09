package services

import javax.inject.{Inject, Singleton}

import com.auth0.client.auth.AuthAPI
import com.auth0.client.mgmt.ManagementAPI
import com.auth0.json.auth.TokenHolder
import com.auth0.json.mgmt.users.User
import play.api.Configuration

import collection.JavaConverters._
/**
  * This class provides a parser for authorization headers and authorization attributes
  * @author Louis Vialar
  */
@Singleton
class Auth0ManagementService @Inject()(config: Configuration) {

  private val domain = config.get[String]("auth0.domain")
  private val clientId = config.get[String]("auth0.clientId")
  private val clientSecret = config.get[String]("auth0.clientSecret")
  private val audience = config.get[String]("auth0.audience")

  private lazy val auth = new AuthAPI(domain, clientId, clientSecret)

  /**
    * Stream of tokens with the timestamp they were created at
    */
  private lazy val tokens: Stream[(Long, TokenHolder)] =
    (auth.requestToken(audience)
        .setScope("read:users update:users")
        .execute match { case t: TokenHolder => (System.currentTimeMillis, t) }
      ) #:: tokens // Self referencing stream

  private def management: ManagementAPI = {
    def generate(tokens: Stream[(Long, TokenHolder)]): TokenHolder = {
      if (tokens.head._2.getExpiresIn == null) tokens.head._2
      else if (tokens.head._1 + tokens.head._2.getExpiresIn * 1000 - 60000 < System.currentTimeMillis) generate(tokens.tail)
      else tokens.head._2
    }

    val token = generate(tokens)
    new ManagementAPI(domain, token.getAccessToken)
  }

  private def doMakeStaff(userId: String, f: Set[String] => Set[String]): Unit = {
    val user = management.users.get(userId, null).execute

    if (user == null) return

    val userUpdate = new User

    val list: Set[String] = Option.apply(user.getAppMetadata) map(_.asScala.get("isStaff")) map {
      case e: Set[String] => e
      case _ => Set[String]()
    } getOrElse Set[String]()

    println("Updating staff status for user " + userId)

    userUpdate.setAppMetadata(Map(("isStaff", f(list).toArray.asInstanceOf[AnyRef])).asJava)
    management.users.update(userId, userUpdate).execute()
  }

  /**
    * Make a user a staff for a given edition. This calls the Auth0 API to add the current edition to the isStaff list
    * in the AppMetadata of the user
    * @param userId the user to update
    * @param edition the edition
    */
  def makeStaff(userId: String, edition: String): Unit = doMakeStaff(userId, _ + edition)
  /**
    * Unmake a user a staff for a given edition. This calls the Auth0 API to remove the current edition from the isStaff
    * list in the AppMetadata of the user
    * @param userId the user to update
    * @param edition the edition
    */
  def unmakeStaff(userId: String, edition: String): Unit = doMakeStaff(userId, _ - edition)

}
