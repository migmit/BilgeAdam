package logic

import cats.effect.IO
import cats.kernel.Monoid
import cats.syntax.either.catsSyntaxEitherId
import cats.syntax.parallel.catsSyntaxParallelFoldMapA
import exceptions.DownloadException
import fs2.Stream
import fs2.data.csv.CsvException
import fs2.data.csv.CsvRowDecoder
import models.Evaluation

import java.time.format.DateTimeParseException

/** Main business logic. Collect data from individual sources using the provided
  * `downloader`, then combine those data.
  *
  * @param downloader
  *   specific way to resolve URLs and collect the data
  */
class BusinessLogic[Url, Item, Stats, Result](
    logic: ModelLogic[Url, Item, Stats, Result],
    downloader: Downloader[Url, Item]
)(using CsvRowDecoder[Item, String], Monoid[Stats]) {

  /** Resolve a URL, parse CSV and collect data on politicians
    *
    * @param url
    *   URL to resolve
    * @return
    *   collected data on each politician
    */
  def handleUrl(url: Url)(using CsvRowDecoder[Item, String]): IO[Stats] =
    for {
      initial <- logic.emptyStats(url)
      result <- downloader
        .getContent(url)
        .handleErrorWith(_ match {
          case (e: DownloadException) =>
            Stream.raiseError[IO](
              new Exception(s"Download error ($url): ${e.getMessage()}")
            )
          case (e: CsvException) =>
            Stream
              .raiseError[IO](
                new Exception(s"Decode error ($url): ${e.getMessage()}")
              )
          case (e: NumberFormatException) =>
            Stream.raiseError[IO](
              new Exception(s"Number parsing error ($url): ${e.getMessage()}")
            )
          case (e: DateTimeParseException) =>
            Stream.raiseError[IO](
              new Exception(s"Date parsing error ($url): ${e.getMessage()}")
            )
          case e =>
            Stream.raiseError[IO](
              new Exception(s"Unexpected error ($url): ${e.getMessage()}")
            )
        })
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
