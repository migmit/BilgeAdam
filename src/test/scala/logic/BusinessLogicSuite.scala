package logic

class BusinessLogicSuite extends munit.CatsEffectSuite {
  val businessLogic =
    new BusinessLogic(new SpeechLogic(), new testData.TestDownloader)

  test("BusinessLogic handles single Url properly") {
    assertIO(businessLogic.handleUrl(testData.csv), testData.statsMap)
  }
  test("BusinessLogic properly processes all incoming data") {
    assertIO(
      businessLogic.combine(List(testData.csv, testData.csv2)),
      testData.evaluation
    )
  }
  test("BusinessLogic fails when one of the URLs fails") {
    assertIOBoolean(
      businessLogic
        .combine(List(testData.csv, "X\nY"))
        .attempt
        .map(_.isLeft)
    )
  }
}
