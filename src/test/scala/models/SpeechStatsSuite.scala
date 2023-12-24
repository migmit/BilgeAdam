package models

import cats.kernel.Monoid
import cats.syntax.monoid.catsSyntaxSemigroup

import java.time.LocalDate

class SpeechStatsSuite extends munit.FunSuite {
  test("updating empty stats works") {
    val speech = Speech("Asimov", "Terminator", LocalDate.of(2000, 1, 1), 123)
    val speechStats = Monoid[SpeechStats].empty
    assertEquals(speechStats.update(speech), SpeechStats(0, 0, 123))
  }
  test("updating empty stats with a 2013 speech works") {
    val speech = Speech("Simak", "Star Wars", LocalDate.of(2013, 12, 31), 321)
    val speechStats = Monoid[SpeechStats].empty
    assertEquals(speechStats.update(speech), SpeechStats(1, 0, 321))
  }
  test("updating empty stats with a speech on security works") {
    val speech =
      Speech("Clark", "Innere Sicherheit", LocalDate.of(1880, 10, 31), 555)
    val speechStats = Monoid[SpeechStats].empty
    assertEquals(speechStats.update(speech), SpeechStats(0, 1, 555))
  }
  test("updating non-empty stats works") {
    val speech =
      Speech("Zelazny", "Innere Sicherheit", LocalDate.of(2013, 4, 1), 4233)
    val speechStats = SpeechStats(
      speechesIn2013 = 7,
      speechesOnSecurity = 11,
      wordsTotal = 27182
    )
    assertEquals(speechStats.update(speech), SpeechStats(8, 12, 31415))
  }
  test("combining empty stats produces empty result") {
    val speechStats = Monoid[SpeechStats].empty
    assertEquals(speechStats |+| speechStats, speechStats)
  }
  test("combining empty stats with something produces that something") {
    val speechStats =
      SpeechStats(speechesIn2013 = 3, speechesOnSecurity = 1, wordsTotal = 3)
    assertEquals(Monoid[SpeechStats].empty |+| speechStats, speechStats)
  }
  test("combining something with empty stats produces that something") {
    val speechStats = SpeechStats(
      speechesIn2013 = 41,
      speechesOnSecurity = 0,
      wordsTotal = 1618
    )
    assertEquals(speechStats |+| Monoid[SpeechStats].empty, speechStats)
  }
  test("combining empty statmaps produces empty result") {
    val statsMap = Monoid[SpeechStatsMap].empty
    assertEquals(statsMap |+| statsMap, statsMap)
  }
  test("combining statmaps containing the same name combines stats") {
    val speechStats1 =
      SpeechStats(speechesIn2013 = 3, speechesOnSecurity = 4, wordsTotal = 5)
    val speechStats2 =
      SpeechStats(speechesIn2013 = 11, speechesOnSecurity = 9, wordsTotal = 100)
    val name = "Clines"
    assertEquals(
      SpeechStatsMap(Map(name -> speechStats1)) |+|
        SpeechStatsMap(Map(name -> speechStats2)),
      SpeechStatsMap(Map(name -> (speechStats1 |+| speechStats2)))
    )
  }
  test("combining statmaps containing different names puts them together") {
    val speechStats1 = SpeechStats(
      speechesIn2013 = 71,
      speechesOnSecurity = 12,
      wordsTotal = 13452
    )
    val speechStats2 = SpeechStats(
      speechesIn2013 = 139,
      speechesOnSecurity = 1,
      wordsTotal = 7967
    )
    val name1 = "Laurence"
    val name2 = "Wells"
    assertEquals(
      SpeechStatsMap(Map(name1 -> speechStats1)) |+|
        SpeechStatsMap(Map(name2 -> speechStats2)),
      SpeechStatsMap(Map(name1 -> speechStats1, name2 -> speechStats2))
    )
  }
}
