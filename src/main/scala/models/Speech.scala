package models

import fs2.data.csv.CsvRowDecoder
import fs2.data.csv.generic.semiauto._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class SpeechRep(
    Redner: String,
    Thema: String,
    Datum: String,
    Wörter: String
)
object SpeechRep {
  given CsvRowDecoder[SpeechRep, String] = deriveCsvRowDecoder
}
final case class Speech(
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
