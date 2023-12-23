import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import config.Http4sConfig
import pureconfig.ConfigSource

/** Main object */
object Main extends IOApp {

  /** Entry point
    * @param args
    *   command line arguments, ignored for now
    * @return
    *   exit code, currently always indicating success
    */
  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigSource.default
    val http4sConfig = config.at("http4s").loadOrThrow[Http4sConfig]
    Server.run(http4sConfig).useForever &> IO(ExitCode.Success)
  }
}
