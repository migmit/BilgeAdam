import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.IO
import cats.effect.kernel.Resource
import com.comcast.ip4s.Port
import config.Http4sConfig
import fs2.Stream
import io.circe.syntax.EncoderOps
import logic.Downloader
import models.Evaluation
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder

object Server {
  import utils.WithOpt._
  val dsl = new Http4sDsl[IO] {}
  import dsl._
  object URLsMatcher
      extends OptionalMultiQueryParamDecoderMatcher[String]("url")
  def routes(
      downloader: Downloader
  ): PartialFunction[Request[IO], IO[Response[IO]]] = {
    case GET -> Root / "evaluation" :? URLsMatcher(vurls) =>
      vurls match {
        case Invalid(e) => IO(Response(Status.BadRequest))
        case Valid(urls) =>
          Stream
            .emits(urls)
            .flatMap(downloader.handleUrl)
            .compile
            .foldMonoid
            .attempt
            .flatMap {
              case Right(statsMap) =>
                Status.Ok(Evaluation.fromStats(statsMap).asJson)
              case Left(e) =>
                Status.InternalServerError(e.getMessage())
            }
      }
  }
  def run(http4sConfig: Http4sConfig): Resource[IO, Unit] = {
    val serverConfig = http4sConfig.server
    val clientConfig = http4sConfig.client
    val clientBuilder = EmberClientBuilder
      .default[IO]
      .withOpt(clientConfig.timeout, _.withTimeout(_))
      .withOpt(clientConfig.idleConnectionTime, _.withIdleConnectionTime(_))
    val serverBuilder = EmberServerBuilder
      .default[IO]
      .withHost(serverConfig.host.toIpAddress.get)
      .withPort(Port.fromInt(serverConfig.port).get)
    for {
      client <- clientBuilder.build
      _ <- serverBuilder
        .withHttpApp(HttpRoutes.of(routes(new Downloader(client))).orNotFound)
        .build
    } yield ()
  }
}
