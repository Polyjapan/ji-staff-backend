package services

import javax.inject.{Inject, Singleton}

import com.auth0.jwt.interfaces.DecodedJWT
import play.api.libs.typedmap.{TypedKey, TypedMap}
import play.api.mvc.{Headers, RequestHeader}

/**
  * @author Louis Vialar
  */
@Singleton
class AuthParserService @Inject()(auth0Service: JwtCheckerService) {

  val authDataKey: TypedKey[DecodedJWT] = TypedKey("AuthData")
  val authCheckKey: TypedKey[Boolean] = TypedKey("HasAuth")

  /**
    * Transform the authorization header (if present) into a TypedMap that can be added as the attributes of a request
    * @param headers the headers
    * @return a map that can be passed as a request attributes
    */
  def getAttributes(headers: Headers): TypedMap = headers.get("Authorization").orNull match {
    case s: String => // The header "Authorization" exists
      if (s.startsWith("Bearer")) { // It's a token (starts with Bearer [token]
        val token = s.drop("Bearer ".length) // Extract the token
        if (auth0Service.isTokenValid(token)) // Check its validity
          TypedMap(authDataKey -> auth0Service.parseToken(token), authCheckKey -> true) // Return the data
        else
          TypedMap(authCheckKey -> false) // In all the other cases the token is invalid or absent => not logged in
      } else {
        TypedMap(authCheckKey -> false)
      }
    case _ => TypedMap(authCheckKey -> false)
  }

  /**
    * Check if the request contains login information
    * @param request the reques
    * @return a tuple, containing the authentication status as the first param (true if logged in) and the
    *         decoded JSON Web Token as the second param, if any
    */
  def isOnline(implicit request: RequestHeader): (Boolean, DecodedJWT) =
    (request.attrs.get(authCheckKey).getOrElse(false), request.attrs.get(authDataKey).orNull)

  def isAdmin(implicit request: RequestHeader): (Boolean, DecodedJWT) =
    isOnline match {
      case (true, jwt) =>
        (
          jwt.getClaim("email_verified").asBoolean() && // Is the e-mail verified (meaning the user controls it)
            jwt.getClaim("email").asString() != null && // Is the e-mail non null
            jwt.getClaim("email").asString().endsWith("@japan-impact.ch") && // is the email belonging to Japan Impact
            jwt.getSubject.startsWith("google"),  // is the user authenticated though Google OAuth2 ?
          jwt)
      case (false, _) => (false, null)
    }

}
