import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.traverse.toTraverseOps
import config.Http4sConfig
import logic.BusinessLogic
import logic.CsvDownloader
import logic.SpeechLogic
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
    val mainResource = for {
      client <- HttpClient.createClient(http4sConfig.client)
      logic = new BusinessLogic(new SpeechLogic(), new CsvDownloader(client))
      server = HttpServer(logic)
      servers <- http4sConfig.server.traverse(server.run)
    } yield ()
    mainResource.use(_ => IO.readLine).map(_ => ExitCode.Success)
  }
}
