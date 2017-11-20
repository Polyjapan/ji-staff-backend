package services

import java.security.interfaces.{RSAPublicKey}
import javax.inject.{Inject, Singleton}

import com.auth0.jwk.{JwkProvider, UrlJwkProvider}
import com.auth0.jwt.{JWT, JWTVerifier}
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.{DecodedJWT, RSAKeyProvider}
import play.api.Configuration

/**
  * This service provides a way to verify and extract the content of JWT (JSON WEB TOKENS)
  * This backend doesn't implement authorization, we rely on Auth0 to do that for us. This service allows us to
  * authenticate the token containing the authorization.
  * @author Louis Vialar
  */
@Singleton
class JwtCheckerService @Inject()(config: Configuration) {
  private val issuer: String = config.get[String]("jwt.issuer")
  private val domain: String = config.get[String]("jwt.domain")

  /**
    * UrlJwkProvider finds our public keys on the authorization server
    */
  private lazy val jwksStore: JwkProvider = new UrlJwkProvider(domain)

  /**
    * The key provider finds our public key in the jwkStore when requested
    */
  private lazy val keyProvider: RSAKeyProvider = new RSAKeyProvider {
    // We don't want to return private keys as our backend will only have to verify signatures
    override def getPrivateKeyId: Nothing = throw UnsupportedOperationException
    override def getPrivateKey: Nothing = throw UnsupportedOperationException

    override def getPublicKeyById(keyId: String): RSAPublicKey = jwksStore.get(keyId).getPublicKey.asInstanceOf[RSAPublicKey]
  }

  private lazy val algorithm: Algorithm = Algorithm.RSA256(keyProvider)
  private lazy val jwtVerifier: JWTVerifier = JWT.require(algorithm).withIssuer(issuer).build()

  /**
    * Checks if a token is valid
    * @param token the token to check
    * @return true if the token is valid, false if it's not
    */
  def isTokenValid(token: String): Boolean = {
    try {
      jwtVerifier.verify(token)
      true
    } catch {
      case _: JWTVerificationException => false
    }
  }

  /**
    * Verifies and arses a token
    * @param token the token to parse & verify
    * @return the parsed token
    * @throws JWTVerificationException if the token is invalid
    */
  def parseToken(token: String): DecodedJWT = {
    jwtVerifier.verify(token)
  }
}
