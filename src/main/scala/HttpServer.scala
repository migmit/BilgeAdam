import cats.data.Validated.Valid
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.traverse.toTraverseOps
import com.comcast.ip4s.Port
import config.ServerConfig
import fs2.io.net.Network
import logic.Combiner
import org.http4s.EntityEncoder.stringEncoder
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

/** HTTP server
  *
  * @param logic
  *   business logic
  */
class HttpServer(logic: Combiner) {
  import utils.WithOpt._
  val dsl = new Http4sDsl[IO] {}
  import dsl._

  /** Match multiple URLs in the query */
  object URLsMatcher
      extends OptionalMultiQueryParamDecoderMatcher[String]("url")

  /** Known routes
    */
  val routes: PartialFunction[Request[IO], IO[Response[IO]]] = {
    case GET -> Root / "evaluation" :? URLsMatcher(vurls) =>
      vurls match {
        case Valid(urls) if urls.nonEmpty =>
          logic
            .combine(urls)
            .attempt
            .flatMap {
              case Right(result) => Status.Ok(result)
              case Left(e)       => Status.InternalServerError(e.getMessage())
            }
        case _ => IO(Response(Status.BadRequest))
      }
  }

  /** Server API
    */
  val httpApp: HttpApp[IO] =
    HttpRoutes.of(routes).orNotFound

  /** Start the server
    *
    * @param serverConfig
    *   configuration for http4s server
    * @return
    *   server resource
    */
  def run(serverConfig: ServerConfig): Resource[IO, Server] = {
    Resource
      .eval(
        serverConfig.tls.traverse { tls =>
          val tlsPassword = tls.keyStorePassword.toCharArray()
          Network[IO].tlsContext
            .fromKeyStoreResource(tls.keyStore, tlsPassword, tlsPassword)
        }
      )
      .map(tlsContext =>
        EmberServerBuilder
          .default[IO]
          .withHost(serverConfig.host.toIpAddress.get)
          .withPort(Port.fromInt(serverConfig.port).get)
          .withOpt(tlsContext, _.withTLS(_))
      )
      .flatMap(_.withHttpApp(httpApp).build)
  }
}
