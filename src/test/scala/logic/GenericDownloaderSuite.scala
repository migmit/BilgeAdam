package logic

import cats.effect.IO
import fs2.Stream
import models.SpeechStatsMap
import models.SpeechStats

class GenericDownloaderSuite extends munit.CatsEffectSuite {

  test("GenericDownloader collects stats properly") {
    assertIO(
      new testData.TestDownloader().handleUrl(testData.csv),
      testData.statsMap
    )
  }
}
