package logic

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Deferred
import cats.effect.kernel.Resource
import cats.syntax.either.catsSyntaxEitherId
import fs2.Stream
import fs2.data.csv.CsvRowDecoder
import fs2.data.csv.CsvRowEncoder
import fs2.data.csv.ParseableHeader
import fs2.data.csv.decodeUsingHeaders
import fs2.data.csv.encodeUsingFirstHeaders
import fs2.data.csv.generic.semiauto._
import models.SpeechRep
import org.http4s.Status
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

object parallelData {
  val dsl = new Http4sDsl[IO] {}
  import dsl._

  def csvToStream(
      csv: String,
      deferred: (Deferred[IO, Unit], Int)
  ): Stream[IO, String] = {
    val count = deferred._2
    given ParseableHeader[String] = ParseableHeader.instance(_.trim().asRight)
    val decoder = CsvRowDecoder[SpeechRep, String]
    given CsvRowEncoder[SpeechRep, String] = deriveCsvRowEncoder
    val records = Stream
      .emit(csv)
      .through[IO, SpeechRep](decodeUsingHeaders())
      .through(encodeUsingFirstHeaders(fullRows = true))
      .compile
      .toList
    Stream
      .eval(records)
      .flatMap(recs =>
        Stream.emits(recs.take(count)) ++ Stream.evals(
          deferred._1.get >> IO(recs.drop(count))
        )
      )
  }

  def client(def1: (Deferred[IO, Unit], Int), def2: (Deferred[IO, Unit], Int)) =
    Client[IO](req =>
      Resource.eval(req.uri.host.map(_.value) match {
        case Some("1") => Status.Ok(csvToStream(testData.csv, def1))
        case Some("2") => Status.Ok(csvToStream(testData.csv2, def2))
        case _         => Status.BadRequest()
      })
    )

}
