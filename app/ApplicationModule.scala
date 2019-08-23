import ch.japanimpact.auth.api.AuthApi
import com.google.inject.{AbstractModule, Provides}
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class ApplicationModule extends AbstractModule {

  /** Module configuration + binding */
  override def configure(): Unit = {}


  @Provides
  def provideAuthClient(ws: WSClient)(implicit ec: ExecutionContext, config: Configuration): AuthApi = AuthApi(ws)

}
