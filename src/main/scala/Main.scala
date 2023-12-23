import EvaluationHelper.updateOneStat
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.either.catsSyntaxEitherId
import fs2.Stream
import fs2.data.csv.CsvException
import fs2.data.csv.CsvRowDecoder
import fs2.data.csv.DecoderError
import fs2.data.csv.ParseableHeader
import fs2.data.csv.decodeUsingHeaders
import fs2.data.csv.generic.semiauto._
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Date

import concurrent.duration.DurationInt

case class SpeechRep(
    Redner: String,
    Thema: String,
    Datum: String,
    Wörter: String
)
case class Speech(
    politician: String,
    subject: String,
    date: LocalDate,
    wordCount: Int
)
object Speech {
  val formatter = DateTimeFormatter.ISO_LOCAL_DATE
  def fromRep(rep: SpeechRep): Speech =
    Speech(
      politician = rep.Redner.trim(),
      subject = rep.Thema.trim(),
      date = LocalDate.parse(rep.Datum.trim(), formatter),
      wordCount = rep.Wörter.trim().toInt
    )
}
case class SpeechStats(
    speechesIn2013: Int,
    speechesOnSecurity: Int,
    wordsTotal: Int
) {
  def update(speech: Speech): SpeechStats = SpeechStats(
    speechesIn2013 =
      (if (speech.date.getYear() == 2013) 1 else 0) + speechesIn2013,
    speechesOnSecurity = (if (speech.subject == SpeechStats.securitySubject) 1
                          else 0) + speechesOnSecurity,
    wordsTotal = wordsTotal + speech.wordCount
  )
  def combine(other: SpeechStats): SpeechStats = SpeechStats(
    speechesIn2013 = speechesIn2013 + other.speechesIn2013,
    speechesOnSecurity = speechesOnSecurity + other.speechesOnSecurity,
    wordsTotal = wordsTotal + other.wordsTotal
  )
}
object SpeechStats {
  val securitySubject = "Innere Sicherheit"
  val initial: SpeechStats =
    SpeechStats(speechesIn2013 = 0, speechesOnSecurity = 0, wordsTotal = 0)
}
case class EvaluationHelper(
    mostSpeeches2013: (Option[String], Int),
    mostSecurity: (Option[String], Int),
    leastWordy: (Option[String], Int)
) {
  def toEvaluation: Evaluation = Evaluation(
    mostSpeeches = mostSpeeches2013._1,
    mostSecurity = mostSecurity._1,
    leastWordy = leastWordy._1
  )
  def update(stats: (String, SpeechStats)): EvaluationHelper = {
    val name = stats._1
    val stat = stats._2
    EvaluationHelper(
      mostSpeeches2013 =
        updateOneStat(mostSpeeches2013, name, stat.speechesIn2013, _ - _),
      mostSecurity =
        updateOneStat(mostSecurity, name, stat.speechesOnSecurity, _ - _),
      leastWordy = updateOneStat(
        leastWordy,
        name,
        stat.wordsTotal,
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
case class Evaluation(
    mostSpeeches: Option[String],
    mostSecurity: Option[String],
    leastWordy: Option[String]
)
object Evaluation {
  def fromStats(stats: Map[String, SpeechStats]): Evaluation =
    stats.foldLeft(EvaluationHelper.initial)(_.update(_)).toEvaluation
}

class DownloadException extends Exception

trait GenericDownloader {
  def getContent(url: String): Stream[IO, String]

  given CsvRowDecoder[SpeechRep, String] = deriveCsvRowDecoder
  given ParseableHeader[String] = ParseableHeader.instance(_.trim().asRight)

  def handleUrl(url: String): Stream[IO, Map[String, SpeechStats]] =
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
      .fold[Map[String, SpeechStats]](Map.empty)((stats, speech) =>
        stats.updated(
          speech.politician,
          stats
            .getOrElse(speech.politician, SpeechStats.initial)
            .update(speech)
        )
      )
}

class Downloader(client: Client[IO]) extends GenericDownloader {
  def getContent(url: String): Stream[IO, String] = client
    .stream(Request(uri = Uri.fromString(url).toOption.get))
    .flatMap(response =>
      if (response.status.isSuccess) response.bodyText
      else Stream.raiseError[IO](new DownloadException)
    )
}

object Main extends IOApp {
  given Encoder[Evaluation] = deriveEncoder
  def merge(
      stats1: Map[String, SpeechStats],
      stats2: Map[String, SpeechStats]
  ): Map[String, SpeechStats] =
    stats1 ++ stats2.map((name, stats) =>
      (
        name,
        stats1.get(name) match {
          case None     => stats
          case Some(st) => stats.combine(st)
        }
      )
    )
  override def run(args: List[String]): IO[ExitCode] = for {
    stats <- EmberClientBuilder
      .default[IO]
      .withTimeout(30.seconds)
      .build
      .use { client =>
        val downloader = new Downloader(client)
        Stream
          .emits(args)
          .flatMap(downloader.handleUrl)
          .compile
          .fold(Map.empty)(merge)
      }
    _ <- IO.println(Evaluation.fromStats(stats).asJson)
  } yield ExitCode.Success
}
