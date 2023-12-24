import cats.effect.IO
import config.ClientConfig
import config.ServerConfig
import config.TLSConfig
import io.circe.Json
import io.circe.parser.parse
import logic.BusinessLogic
import logic.CsvDownloader
import logic.SpeechLogic
import logic.testData
import org.http4s.Request
import org.http4s.Uri
import org.http4s.circe.jsonDecoder
import org.http4s.dsl.Http4sDsl

import scala.concurrent.duration.Duration

import concurrent.duration.DurationInt

class ServerSuite extends munit.CatsEffectSuite {
  val dsl = new Http4sDsl[IO] {}
  import dsl._

  val testServer = HttpServer(
    new BusinessLogic(new SpeechLogic(), new CsvDownloader(testData.client))
  )

  test("Http app should resolve URLs properly") {
    assertIO(
      testServer.httpApp
        .run(
          Request(uri =
            Uri.fromString("evaluation?url=http://1&url=http://2").toOption.get
          )
        )
        .flatMap(resp =>
          resp.body.compile.toList.map(list => (list, resp.status.isSuccess))
        )
        .map(result => (parse(new String(result._1.toArray)), result._2)),
      (Right(testData.json), true)
    )
  }

  test("Http app should fail if one of URLs is unresolvable") {
    assertIOBoolean(
      testServer.httpApp
        .run(
          Request(uri =
            Uri.fromString("evaluation?url=http://3&url=http://1").toOption.get
          )
        )
        .map(!_.status.isSuccess)
    )
  }

  test("Http app should reject calls without URLs") {
    assertIOBoolean(
      testServer.httpApp
        .run(Request(uri = Uri.fromString("evaluation").toOption.get))
        .map(!_.status.isSuccess)
    )
  }

  test("Server should work over HTTP") {
    val localhost = Uri.Host.fromString("0.0.0.0").toOption.get
    val portNumber = 27182
    val client = HttpClient.createClient(
      ClientConfig(timeout = Some(30.seconds), idleConnectionTime = None)
    )
    assertIO(
      testServer
        .run(ServerConfig(localhost, portNumber, None))
        .use(_ =>
          client.use(
            _.expect[Json](
              s"http://localhost:$portNumber/evaluation?url=http://1&url=http://2"
            )
          )
        ),
      testData.json
    )
  }
  test("Server should work over HTTPS") {
    val localhost = Uri.Host.fromString("0.0.0.0").toOption.get
    val portNumber = 31415
    val tlsConfig = TLSConfig("keyStore.p12", "passphrase")
    val client = HttpClient.createClient(
      ClientConfig(timeout = None, idleConnectionTime = Some(Duration.Inf)),
      Some(tlsConfig)
    )
    assertIO(
      testServer
        .run(ServerConfig(localhost, portNumber, Some(tlsConfig)))
        .use(_ =>
          client.use(
            _.expect[Json](
              s"https://localhost:$portNumber/evaluation?url=http://1&url=http://2"
            )
          )
        ),
      testData.json
    )
  }
}
