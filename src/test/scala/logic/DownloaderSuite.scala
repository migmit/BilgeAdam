package logic

class DownloaderSuite extends munit.CatsEffectSuite {
  val downloader = new CsvDownloader(testData.client)
  test("Downloader should work") {
    assertIO(
      downloader.getContent("http://1").compile.toList,
      testData.statsList
    )
  }
  test("Downloader should handle invalid URLs gracefully") {
    assertIOBoolean(
      downloader.getContent(":").compile.toList.attempt.map(_.isLeft)
    )
  }
  test("Downloader should fail gracefully if the URL is unresolvable") {
    assertIOBoolean(
      downloader.getContent("http://3").compile.toList.attempt.map(_.isLeft)
    )
  }
}
