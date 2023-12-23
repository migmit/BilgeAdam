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

trait GenericDownloader {
  def getContent(url: String): Stream[IO, String]

  given ParseableHeader[String] = ParseableHeader.instance(_.trim().asRight)

  def handleUrl(url: String): Stream[IO, SpeechStatsMap] =
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
      .fold[Map[String, SpeechStats]](Map.empty)((allStats, speech) =>
        allStats.updated(
          speech.politician,
          allStats
            .getOrElse(speech.politician, Monoid[SpeechStats].empty)
            .update(speech)
        )
      )
      .map(SpeechStatsMap(_))
}
