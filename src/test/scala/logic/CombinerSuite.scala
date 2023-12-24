package logic

class CombinerSuite extends munit.CatsEffectSuite {
  test("Combiner properly processes all incoming data") {
    assertIO(
      new Combiner(new testData.TestDownloader)
        .combine(List(testData.csv, testData.csv2)),
      testData.evaluation
    )
  }
  test("Combiner fails when one of the URLs fails") {
    assertIOBoolean(
      new Combiner(new testData.TestDownloader)
        .combine(List(testData.csv, "X\nY"))
        .attempt
        .map(_.isLeft)
    )
  }
}
