package models

import cats.kernel.Monoid
import cats.syntax.monoid.catsSyntaxSemigroup

/** Data we need to track for any specific politician
  *
  * @param speechesIn2013
  *   Number of speeches that politician gave in 2013
  * @param speechesOnSecurity
  *   Number of speeches that politician gave with the subject being
  *   SpeechStats.securitySubject
  * @param wordsTotal
  *   Total number of all speeches given by that politician
  */
final case class SpeechStats(
    speechesIn2013: Int,
    speechesOnSecurity: Int,
    wordsTotal: Int
) {

  /** Add one more speech
    * @param speech
    *   data to add
    * @returns
    *   updated stats
    */
  def update(speech: Speech): SpeechStats = SpeechStats(
    speechesIn2013 =
      (if (speech.date.getYear() == 2013) 1 else 0) + speechesIn2013,
    speechesOnSecurity = (if (speech.subject == SpeechStats.securitySubject) 1
                          else 0) + speechesOnSecurity,
    wordsTotal = wordsTotal + speech.wordCount
  )
}
object SpeechStats {

  /** Subject to pay attention to */
  val securitySubject = "Innere Sicherheit"
  given Monoid[SpeechStats] = new Monoid[SpeechStats] {
    override def combine(x: SpeechStats, y: SpeechStats): SpeechStats =
      SpeechStats(
        speechesIn2013 = x.speechesIn2013 + y.speechesIn2013,
        speechesOnSecurity = x.speechesOnSecurity + y.speechesOnSecurity,
        wordsTotal = x.wordsTotal + y.wordsTotal
      )
    override def empty: SpeechStats =
      SpeechStats(speechesIn2013 = 0, speechesOnSecurity = 0, wordsTotal = 0)
  }
}

/** Overall statistics for each politician
  *
  * @param allStats
  *   map, showing collected data on each politician
  */
final case class SpeechStatsMap(allStats: Map[String, SpeechStats])
object SpeechStatsMap {
  given Monoid[SpeechStatsMap] =
    new Monoid[SpeechStatsMap] {
      override def combine(
          x: SpeechStatsMap,
          y: SpeechStatsMap
      ): SpeechStatsMap = SpeechStatsMap(
        x.allStats ++ y.allStats.map((name, stats) =>
          (
            name,
            x.allStats.get(name) match {
              case None     => stats
              case Some(st) => stats |+| st
            }
          )
        )
      )
      override def empty: SpeechStatsMap = SpeechStatsMap(Map.empty)
    }
}
