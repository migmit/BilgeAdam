package logic

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.either.catsSyntaxEitherId
import fs2.Stream
import fs2.data.csv.ParseableHeader
import fs2.data.csv.decodeUsingHeaders
import io.circe.parser.parse
import models.Evaluation
import models.Speech
import models.SpeechStats
import models.SpeechStatsMap
import org.http4s.Status
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

import java.time.LocalDate

object testData {
  class TestDownloader extends Downloader[String, Speech] {
    given ParseableHeader[String] = ParseableHeader.instance(_.trim().asRight)
    override def getContent(url: String): Stream[IO, Speech] =
      Stream.emit(url).through[IO, Speech](decodeUsingHeaders())
  }

  val csv = """Redner, Thema, Datum, Wörter
  |Wolfgang Amadeus Mozart, Innere Sicherheit, 2013-05-09, 758
  |Georges Bizet, Innere Sicherheit, 2012-06-12, 959
  |Pyotr Ilyich Tchaikovsky, Love and Thunder, 2017-09-01, 89
  |Georges Bizet, Innere Sicherheit, 2013-10-02, 715""".stripMargin

  val statsMap = SpeechStatsMap(
    Map(
      "Wolfgang Amadeus Mozart" -> SpeechStats(
        speechesIn2013 = 1,
        speechesOnSecurity = 1,
        wordsTotal = 758
      ),
      "Georges Bizet" -> SpeechStats(
        speechesIn2013 = 1,
        speechesOnSecurity = 2,
        wordsTotal = 1674
      ),
      "Pyotr Ilyich Tchaikovsky" -> SpeechStats(
        speechesIn2013 = 0,
        speechesOnSecurity = 0,
        wordsTotal = 89
      )
    )
  )

  val statsList: List[Speech] =
    List(
      Speech(
        politician = "Wolfgang Amadeus Mozart",
        subject = "Innere Sicherheit",
        date = LocalDate.of(2013, 5, 9),
        wordCount = 758
      ),
      Speech(
        politician = "Georges Bizet",
        subject = "Innere Sicherheit",
        date = LocalDate.of(2012, 6, 12),
        wordCount = 959
      ),
      Speech(
        politician = "Pyotr Ilyich Tchaikovsky",
        subject = "Love and Thunder",
        date = LocalDate.of(2017, 9, 1),
        wordCount = 89
      ),
      Speech(
        politician = "Georges Bizet",
        subject = "Innere Sicherheit",
        date = LocalDate.of(2013, 10, 2),
        wordCount = 715
      )
    )

  val csv2 = """Redner, Thema, Datum, Wörter
  |Wolfgang Amadeus Mozart, Civil War, 2013-02-24, 551
  |Wolfgang Amadeus Mozart, Innere Sicherheit, 2014-01-09, 201""".stripMargin

  val evaluation =
    Evaluation(
      mostSpeeches = Some("Wolfgang Amadeus Mozart"),
      mostSecurity = None,
      leastWordy = Some("Pyotr Ilyich Tchaikovsky")
    )

  val json = parse(
    """{"mostSpeeches":"Wolfgang Amadeus Mozart","mostSecurity":null,"leastWordy":"Pyotr Ilyich Tchaikovsky"}"""
  ).toOption.get

  val dsl = new Http4sDsl[IO] {}
  import dsl._

  val client = Client[IO](req =>
    Resource.eval(req.uri.host.map(_.value) match {
      case Some("1") => Status.Ok(testData.csv)
      case Some("2") => Status.Ok(testData.csv2)
      case _         => Status.BadRequest()
    })
  )
}
