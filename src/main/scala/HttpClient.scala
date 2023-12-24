import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.traverse.toTraverseOps
import config.ClientConfig
import config.TLSConfig
import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

/** Helper object for creating an HTTP client
  */
object HttpClient {
  import utils.WithOpt._

  /** Create client from configuration
    *
    * @param clientConfig
    *   configuration for http4s client
    * @param tlsConfig
    *   custom configuration for HTTPS, if necessary (usually not)
    * @return
    *   HTTP client
    */
  def createClient(
      clientConfig: ClientConfig,
      tlsConfig: Option[TLSConfig] = None
  ): Resource[IO, Client[IO]] =
    tlsConfig
      .traverse { tls =>
        val tlsPassword = tls.keyStorePassword.toCharArray()
        Resource.eval(
          Network[IO].tlsContext
            .fromKeyStoreResource(tls.keyStore, tlsPassword, tlsPassword)
        )
      }
      .flatMap(tlsContext =>
        EmberClientBuilder
          .default[IO]
          .withOpt(clientConfig.timeout, _.withTimeout(_))
          .withOpt(clientConfig.idleConnectionTime, _.withIdleConnectionTime(_))
          .withOpt(tlsContext, _.withTLSContext(_))
          .withCheckEndpointAuthentication(tlsConfig.isEmpty)
          .build
      )
}
