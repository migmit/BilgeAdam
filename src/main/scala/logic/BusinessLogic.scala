package logic

import cats.effect.IO
import cats.kernel.Monoid
import cats.syntax.either.catsSyntaxEitherId
import cats.syntax.parallel.catsSyntaxParallelFoldMapA
import exceptions.URLException
import fs2.Stream
import fs2.data.csv.CsvException
import fs2.data.csv.CsvRowDecoder
import models.Evaluation

import java.time.format.DateTimeParseException

/** Main business logic. Collect data from individual sources using the provided
  * `downloader`, then combine those data.
  *
  * @param downloader
  *   specific way to resolve URLs
  */
class BusinessLogic[Url, Item, Stats, Result](
    logic: ModelLogic[Url, Item, Stats, Result],
    downloader: Downloader[Url, Item]
)(using CsvRowDecoder[Item, String], Monoid[Stats]) {

  /** Resolve a URL, parse everything and collect data
    *
    * @param url
    *   URL to resolve
    * @return
    *   collected data
    */
  def handleUrl(url: Url)(using CsvRowDecoder[Item, String]): IO[Stats] =
    for {
      initial <- logic.emptyStats(url)
      result <- downloader
        .getContent(url)
        .handleErrorWith(error =>
          Stream.raiseError[IO](new URLException(url, error))
        )
        .evalScan[IO, Stats](initial)((allStats, speech) =>
          logic.updateStats(url, allStats, speech)
        )
        .compile
        .lastOrError
    } yield result

  /** Resolve given URLs and combine all collected data
    *
    * @param urls
    *   data sources
    * @return
    *   result of collecting and processing all the data
    */
  def combine(urls: Seq[Url]): IO[Result] =
    urls
      .parFoldMapA(handleUrl)
      .flatMap(logic.finalizeStats)
}
