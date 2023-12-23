package models

import io.circe.Encoder
import io.circe.generic.semiauto._

/** Processed data for some (not all) politicians
  *
  * @param mostSpeeches2013
  *   name of a policitian who gave the biggest number of speaches in 2013, and
  *   the number itself
  * @param mostSecurity
  *   name of a politician who gave the biggest number of speeches on security,
  *   and the number itself
  * @param leastWordy
  *   name of a politician who used the least number of words in their speeches,
  *   and the number itself
  */
final case class EvaluationHelper(
    mostSpeeches2013: (Option[String], Int),
    mostSecurity: (Option[String], Int),
    leastWordy: (Option[String], Int)
) {

  /** Remove numbers and leave the names only
    *
    * @return
    *   names of politicians with record numbers
    */
  def toEvaluation: Evaluation = Evaluation(
    mostSpeeches = mostSpeeches2013._1,
    mostSecurity = mostSecurity._1,
    leastWordy = leastWordy._1
  )

  /** Add data for another politician
    *
    * @param namedStats
    *   name of the politician and their collected data
    * @return
    *   updated processed data
    */
  def update(namedStats: (String, SpeechStats)): EvaluationHelper = {
    val name = namedStats._1
    val stats = namedStats._2
    EvaluationHelper(
      mostSpeeches2013 = EvaluationHelper
        .updateOneStat(mostSpeeches2013, name, stats.speechesIn2013, _ - _),
      mostSecurity = EvaluationHelper
        .updateOneStat(mostSecurity, name, stats.speechesOnSecurity, _ - _),
      leastWordy = EvaluationHelper.updateOneStat(
        leastWordy,
        name,
        stats.wordsTotal,
        (n, o) => if (o < 0) then 1 else o - n
      )
    )
  }
}
object EvaluationHelper {

  /** Helper function to update one part of processed data
    *
    * @param acc
    *   name of a politician with the best stat and the stat itself
    * @param name
    *   name of a new politician
    * @param stat
    *   stat of a new politician
    * @param compare
    *   function to compare stats; returns positive number if the first stat is
    *   better than the second, and negative if it's worse
    */
  def updateOneStat(
      acc: (Option[String], Int),
      name: String,
      stat: Int,
      compare: (Int, Int) => Int
  ): (Option[String], Int) = {
    val cmp = compare(stat, acc._2)
    if (cmp > 0) (Some(name), stat) else if (cmp < 0) acc else (None, stat)
  }

  /** Processed data in the very beginning of the processing */
  val initial: EvaluationHelper = EvaluationHelper(
    mostSpeeches2013 = (None, -1),
    mostSecurity = (None, -1),
    leastWordy = (None, -1)
  )
}

/** Processed data for all politicians, ready to be given to the user
  *
  * @param mostSpeeches
  *   name of a policitian who gave the biggest number of speaches in 2013
  * @param mostSecurity
  *   name of a politician who gave the biggest number of speeches on security
  * @param leastWordy
  *   name of a politician who used the least number of words in their speeches
  */
final case class Evaluation(
    mostSpeeches: Option[String],
    mostSecurity: Option[String],
    leastWordy: Option[String]
)
object Evaluation {

  /** Process collected data for all politician
    *
    * @param stats
    *   overall statistics for each politician
    */
  def fromStats(statsMap: SpeechStatsMap): Evaluation =
    statsMap.allStats
      .foldLeft(EvaluationHelper.initial)(_.update(_))
      .toEvaluation
  given Encoder[Evaluation] = deriveEncoder
}
