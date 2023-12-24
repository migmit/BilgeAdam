package logic

import cats.effect.IO
import cats.kernel.Monoid
import cats.syntax.either.catsSyntaxEitherId
import exceptions.DownloadException
import fs2.Stream
import fs2.data.csv.CsvException
import fs2.data.csv.ParseableHeader
import fs2.data.csv.decodeUsingHeaders
import models.Speech
import models.SpeechRep
import models.SpeechStats
import models.SpeechStatsMap

import java.time.format.DateTimeParseException

/** Common interface for resolving URLs, downloading CSVs and collecting data on
  * politicians
  */
trait GenericDownloader {

  /** Resolve a URL, producing the text stream
    *
    * @param url
    *   URL to resolve
    * @return
    *   text stream
    */
  def getContent(url: String): Stream[IO, String]

  /** Method to update stats, can be overwritten
    *
    * @param url
    *   current download url, usually ignored
    * @param stats
    *   existing stats
    * @param speech
    *   new speach' data
    * @return
    *   updated stats
    */
  def update(url: String, stats: SpeechStats, speech: Speech): IO[SpeechStats] =
    IO(stats.update(speech))

  given ParseableHeader[String] = ParseableHeader.instance(_.trim().asRight)

  /** Resolve a URL, parse CSV and collect data on politicians
    *
    * @param url
    *   URL to resolve
    * @return
    *   collected data on each politician
    */
  def handleUrl(url: String): IO[SpeechStatsMap] =
    getContent(url)
      .through(decodeUsingHeaders[SpeechRep]())
      .map(Speech.fromRep)
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
      .evalScan[IO, Map[String, SpeechStats]](Map.empty)((allStats, speech) =>
        update(
          url,
          allStats.getOrElse(speech.politician, Monoid[SpeechStats].empty),
          speech
        ).map(allStats.updated(speech.politician, _))
      )
      .compile
      .lastOrError
      .map(SpeechStatsMap(_))
}
