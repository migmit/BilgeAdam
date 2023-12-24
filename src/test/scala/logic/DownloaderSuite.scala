package logic

class DownloaderSuite extends munit.CatsEffectSuite {
  val downloader = new Downloader(testData.client)
  test("Downloader should work") {
    assertIO(downloader.handleUrl("http://1"), testData.statsMap)
  }
  test("Downloader should handle invalid URLs gracefully") {
    assertIOBoolean(downloader.handleUrl(":").attempt.map(_.isLeft))
  }
  test("Downloader should fail gracefully if the URL is unresolvable") {
    assertIOBoolean(downloader.handleUrl("http://3").attempt.map(_.isLeft))
  }
}
