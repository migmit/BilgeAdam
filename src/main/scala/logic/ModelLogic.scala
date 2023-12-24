package logic

import cats.effect.IO
import cats.kernel.Monoid
import models.Evaluation
import models.Speech
import models.SpeechStats
import models.SpeechStatsMap

trait ModelLogic[Url, Item, Stats, Result] {
  def emptyStats(url: Url): IO[Stats]
  def updateStats(url: Url, stats: Stats, item: Item): IO[Stats]
  def finalizeStats(stats: Stats): IO[Result]
}

class SpeechLogic
    extends ModelLogic[String, Speech, SpeechStatsMap, Evaluation] {
  val ssmMonoid = Monoid[SpeechStatsMap]
  val statsMonoid = Monoid[SpeechStats]
  def emptyStats(url: String): IO[SpeechStatsMap] = IO(ssmMonoid.empty)
  def updateStats(
      url: String,
      stats: SpeechStatsMap,
      item: Speech
  ): IO[SpeechStatsMap] =
    IO(
      SpeechStatsMap(
        stats.allStats.updated(
          item.politician,
          stats.allStats
            .getOrElse(item.politician, statsMonoid.empty)
            .update(item)
        )
      )
    )
  def finalizeStats(stats: SpeechStatsMap): IO[Evaluation] =
    IO(Evaluation.fromStats(stats))
}
