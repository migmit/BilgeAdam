package models

import fs2.data.csv.CsvRowDecoder
import fs2.data.csv.generic.semiauto._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Representation as in CSV file, not trimmed
  *
  * @param Redner
  *   speaker's name
  * @param Thema
  *   the speech subject
  * @param Datum
  *   the date of the speech
  * @param Wörter
  *   the number of words in the speech
  */
final case class SpeechRep(
    Redner: String,
    Thema: String,
    Datum: String,
    Wörter: String
)
object SpeechRep {
  given CsvRowDecoder[SpeechRep, String] = deriveCsvRowDecoder
}

/** More natural representation, trimmed and converted to the suitable data
  * types
  *
  * @param politician
  *   speaker's name
  * @param subject
  *   the speech subject
  * @param date
  *   the date of the speech
  * @param wordCount
  *   the number of words in the speech
  */
final case class Speech(
    politician: String,
    subject: String,
    date: LocalDate,
    wordCount: Int
)
object Speech {
  given CsvRowDecoder[Speech, String] =
    CsvRowDecoder[SpeechRep, String].map(Speech.fromRep)

  val formatter = DateTimeFormatter.ISO_LOCAL_DATE

  /** Convert CSV representation to the natural one, trimming all lines
    *
    * @param rep
    *   CSV representation
    * @return
    *   natural representation
    */
  def fromRep(rep: SpeechRep): Speech =
    Speech(
      politician = rep.Redner.trim(),
      subject = rep.Thema.trim(),
      date = LocalDate.parse(rep.Datum.trim(), formatter),
      wordCount = rep.Wörter.trim().toInt
    )
}
