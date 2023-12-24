import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.option.catsSyntaxOptionId
import cats.syntax.traverse.toTraverseOps
import com.comcast.ip4s.Port
import config.Http4sConfig
import config.ServerConfig
import fs2.io.net.Network
import logic.Combiner
import logic.Downloader
import org.http4s.EntityEncoder.stringEncoder
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder

/** HTTP server
  */
object Server {
  import utils.WithOpt._
  val dsl = new Http4sDsl[IO] {}
  import dsl._

  /** Match multiple URLs in the query */
  object URLsMatcher
      extends OptionalMultiQueryParamDecoderMatcher[String]("url")

  /** Known routes
    *
    * @param combiner
    *   business logic, processing URLs
    */
  def routes(
      combiner: Combiner
  ): PartialFunction[Request[IO], IO[Response[IO]]] = {
    case GET -> Root / "evaluation" :? URLsMatcher(vurls) =>
      vurls match {
        case Invalid(e) => IO(Response(Status.BadRequest))
        case Valid(urls) =>
          combiner
            .combine(urls)
            .attempt
            .flatMap {
              case Right(result) => Status.Ok(result)
              case Left(e)       => Status.InternalServerError(e.getMessage())
            }
      }
  }

  /** Server API
    *
    * @param combiner
    *   business logic, processing URLs
    */
  def httpApp(combiner: Combiner): HttpApp[IO] =
    HttpRoutes.of(routes(combiner)).orNotFound

  /** Start the server
    *
    * @param http4sConfig
    *   configuration for http4s resources
    * @return
    *   server resource
    */
  def run(http4sConfig: Http4sConfig): Resource[IO, Unit] = {
    val serverConfigs = http4sConfig.server
    val clientConfig = http4sConfig.client
    val clientBuilder = EmberClientBuilder
      .default[IO]
      .withOpt(clientConfig.timeout, _.withTimeout(_))
      .withOpt(clientConfig.idleConnectionTime, _.withIdleConnectionTime(_))
    def serverBuilder(config: ServerConfig) = Resource
      .eval(
        config.tls.traverse { tls =>
          val tlsPassword = tls.keyStorePassword.toCharArray()
          Network[IO].tlsContext
            .fromKeyStoreResource(tls.keyStore, tlsPassword, tlsPassword)
        }
      )
      .map(tlsContext =>
        EmberServerBuilder
          .default[IO]
          .withHost(config.host.toIpAddress.get)
          .withPort(Port.fromInt(config.port).get)
          .withOpt(tlsContext, _.withTLS(_))
      )
    for {
      client <- clientBuilder.build
      combiner = new Combiner(new Downloader(client))
      _ <- serverConfigs.traverse(
        serverBuilder(_).flatMap(_.withHttpApp(httpApp(combiner)).build)
      )
    } yield ()
  }
}
