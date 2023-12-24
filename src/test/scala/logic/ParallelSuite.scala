package logic

import cats.effect.IO
import cats.effect.kernel.Deferred
import models.Speech
import models.SpeechStats
import org.http4s.client.Client

class TestParallelDownloader(
    client: Client[IO],
    def1: Deferred[IO, Unit],
    def2: Deferred[IO, Unit]
) extends Downloader(client) {
  override def update(
      url: String,
      stats: SpeechStats,
      speech: Speech
  ): IO[SpeechStats] =
    for {
      _ <- url match {
        case "http://1" => def2.complete(())
        case "http://2" => def1.complete(())
        case _          => IO(true)
      }
      result <- super.update(url, stats, speech)
    } yield result
}

class ParallelSuite extends munit.CatsEffectSuite {
  test("Combiner processes multiple streams in parallel") {
    assertIO(
      for {
        def1 <- Deferred[IO, Unit]
        def2 <- Deferred[IO, Unit]
        combiner <- new Combiner(
          new TestParallelDownloader(
            parallelData.client(def1 -> 3, def2 -> 2),
            def1,
            def2
          )
        ).combine(List("http://1", "http://2"))
      } yield combiner,
      testData.evaluation
    )
  }
}
