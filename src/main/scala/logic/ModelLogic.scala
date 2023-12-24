package logic

import cats.effect.IO
import cats.kernel.Monoid
import models.Evaluation
import models.Speech
import models.SpeechStats
import models.SpeechStatsMap

/** Various ways to combine and process the data
  */
trait ModelLogic[Url, Item, Stats, Result] {

  /** The state in the beginning of the processing
    *
    * @param url
    *   URL of the new stream
    * @return
    *   empty stats
    */
  def emptyStats(url: Url): IO[Stats]

  /** Append one more item to the stats
    *
    * @param url
    *   URL of the stream
    * @param stats
    *   stats before appending
    * @param item
    *   new item
    * @return
    *   updated stats
    */
  def updateStats(url: Url, stats: Stats, item: Item): IO[Stats]

  /** Produce the final result
    *
    * @param stats
    *   collected data after the stream is finished
    * @returns
    *   final result
    */
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
