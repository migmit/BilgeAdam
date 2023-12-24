package models

import io.circe.Encoder
import logic.testData

import java.time.LocalDate

class EvaluationSuite extends munit.FunSuite {
  test("EvaluationHelper converts to Evaluation correctly when it is empty") {
    val evaluationHelper = EvaluationHelper.initial
    assertEquals(
      evaluationHelper.toEvaluation,
      Evaluation(mostSpeeches = None, mostSecurity = None, leastWordy = None)
    )
  }
  test(
    "EvaluationHelper converts to Evaluation correctly when names are unknown"
  ) {
    val evaluationHelper = EvaluationHelper(
      mostSpeeches2013 = (None, 30),
      mostSecurity = (None, 13),
      leastWordy = (None, 12)
    )
    assertEquals(
      evaluationHelper.toEvaluation,
      Evaluation(mostSpeeches = None, mostSecurity = None, leastWordy = None)
    )
  }
  test(
    "EvaluationHelper converts to Evaluation correctly when names are known"
  ) {
    val mostSpeeches2013 = "Holmes"
    val mostSecurity = "Poirot"
    val leastWordy = "Wimsey"
    val evaluationHelper = EvaluationHelper(
      mostSpeeches2013 = (Some(mostSpeeches2013), 55),
      mostSecurity = (Some(mostSecurity), 87),
      leastWordy = (Some(leastWordy), 11)
    )
    assertEquals(
      evaluationHelper.toEvaluation,
      Evaluation(
        mostSpeeches = Some(mostSpeeches2013),
        mostSecurity = Some(mostSecurity),
        leastWordy = Some(leastWordy)
      )
    )
  }
  test("Updating initial EvaluationHelper works") {
    val evaluationHelper = EvaluationHelper.initial
    val name = "Fell"
    val speechesIn2013 = 5
    val speechesOnSecurity = 3
    val wordsTotal = 64
    val speechStats =
      SpeechStats(
        speechesIn2013 = speechesIn2013,
        speechesOnSecurity = speechesOnSecurity,
        wordsTotal = wordsTotal
      )
    assertEquals(
      evaluationHelper.update((name, speechStats)),
      EvaluationHelper(
        mostSpeeches2013 = (Some(name), speechesIn2013),
        mostSecurity = (Some(name), speechesOnSecurity),
        leastWordy = (Some(name), wordsTotal)
      )
    )
  }
  test("Updating non-initial EvaluationHelper works") {
    val name1 = "Maigret"
    val name2 = "Brown"
    val name = "Dupin"
    val oldSpeechesIn2013 = 8
    val oldSpeechesOnSecurity = 12
    val leastWordy = 400
    val newSpeechesIn2013 = 9
    val newSpeechesOnSecurity = 11
    val evaluationHelper = EvaluationHelper(
      mostSpeeches2013 = (Some(name1), oldSpeechesIn2013),
      mostSecurity = (None, oldSpeechesOnSecurity),
      leastWordy = (Some(name2), leastWordy)
    )
    val speechStats = SpeechStats(
      speechesIn2013 = newSpeechesIn2013,
      speechesOnSecurity = newSpeechesOnSecurity,
      wordsTotal = leastWordy
    )
    assertEquals(
      evaluationHelper.update((name, speechStats)),
      EvaluationHelper(
        mostSpeeches2013 = (Some(name), newSpeechesIn2013),
        mostSecurity = (None, oldSpeechesOnSecurity),
        leastWordy = (None, leastWordy)
      )
    )
  }
  test("Processing speech data works") {
    val speechStats = SpeechStatsMap(
      Map(
        "Fletcher" -> SpeechStats(
          speechesIn2013 = 13,
          speechesOnSecurity = 6,
          wordsTotal = 3412
        ),
        "Mason" -> SpeechStats(
          speechesIn2013 = 10,
          speechesOnSecurity = 6,
          wordsTotal = 5867
        ),
        "Clouseau" -> SpeechStats(
          speechesIn2013 = 10,
          speechesOnSecurity = 5,
          wordsTotal = 100
        ),
        "Batman" -> SpeechStats(
          speechesIn2013 = 1,
          speechesOnSecurity = 1,
          wordsTotal = 1000
        )
      )
    )
    assertEquals(
      Evaluation.fromStats(speechStats),
      Evaluation(
        mostSpeeches = Some("Fletcher"),
        mostSecurity = None,
        leastWordy = Some("Clouseau")
      )
    )
  }
  test("JSON encoding of Evaluation works") {
    val evaluationCoder = Encoder[Evaluation]
    assertEquals(evaluationCoder(testData.evaluation), testData.json)
  }
}
