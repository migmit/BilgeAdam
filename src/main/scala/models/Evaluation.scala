package models

import io.circe.Encoder
import io.circe.generic.semiauto._

final case class EvaluationHelper(
    mostSpeeches2013: (Option[String], Int),
    mostSecurity: (Option[String], Int),
    leastWordy: (Option[String], Int)
) {
  def toEvaluation: Evaluation = Evaluation(
    mostSpeeches = mostSpeeches2013._1,
    mostSecurity = mostSecurity._1,
    leastWordy = leastWordy._1
  )
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
  def updateOneStat(
      acc: (Option[String], Int),
      name: String,
      stat: Int,
      compare: (Int, Int) => Int
  ): (Option[String], Int) = {
    val cmp = compare(stat, acc._2)
    if (cmp > 0) (Some(name), stat) else if (cmp < 0) acc else (None, stat)
  }
  val initial: EvaluationHelper = EvaluationHelper(
    mostSpeeches2013 = (None, -1),
    mostSecurity = (None, -1),
    leastWordy = (None, -1)
  )
}
final case class Evaluation(
    mostSpeeches: Option[String],
    mostSecurity: Option[String],
    leastWordy: Option[String]
)
object Evaluation {
  def fromStats(statsMap: SpeechStatsMap): Evaluation =
    statsMap.allStats
      .foldLeft(EvaluationHelper.initial)(_.update(_))
      .toEvaluation
  given Encoder[Evaluation] = deriveEncoder
}
