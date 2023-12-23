import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import config.Http4sConfig
import pureconfig.ConfigSource

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigSource.default
    val http4sConfig = config.at("http4s").loadOrThrow[Http4sConfig]
    Server.run(http4sConfig).useForever &> IO(ExitCode.Success)
  }
}
