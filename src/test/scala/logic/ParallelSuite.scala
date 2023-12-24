package logic

import cats.effect.IO
import cats.effect.kernel.Deferred
import models.Speech
import models.SpeechStats
import models.SpeechStatsMap
import org.http4s.client.Client

class TestParallelLogic(
    def1: Deferred[IO, Unit],
    def2: Deferred[IO, Unit]
) extends SpeechLogic {
  override def updateStats(
      url: String,
      stats: SpeechStatsMap,
      speech: Speech
  ): IO[SpeechStatsMap] =
    for {
      _ <- url match {
        // Allow each stream to proceed
        // only after the first item in the other stream is already read
        case "http://1" => def2.complete(())
        case "http://2" => def1.complete(())
        case _          => IO(true)
      }
      result <- super.updateStats(url, stats, speech)
    } yield result
}

class ParallelSuite extends munit.CatsEffectSuite {
  test("Combiner processes multiple streams in parallel") {
    assertIO(
      for {
        def1 <- Deferred[IO, Unit]
        def2 <- Deferred[IO, Unit]
        combiner <- new BusinessLogic(
          new TestParallelLogic(def1, def2),
          new CsvDownloader(parallelData.client(def1 -> 3, def2 -> 2))
        ).combine(List("http://1", "http://2"))
      } yield combiner,
      testData.evaluation
    )
  }
}
