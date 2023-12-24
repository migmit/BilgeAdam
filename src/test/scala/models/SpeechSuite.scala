package models

import cats.data.NonEmptyList
import fs2.data.csv.CsvRow
import fs2.data.csv.CsvRowDecoder

import java.time.LocalDate

class SpeechSuite extends munit.FunSuite {
  test("Decoding from CSV works") {
    val decoder = CsvRowDecoder[SpeechRep, String]
    val speechRep = SpeechRep("Vim", "Nothing", "Never", "Too many")
    assertEquals(
      decoder(
        CsvRow(
          NonEmptyList.of(
            speechRep.Redner,
            speechRep.Thema,
            speechRep.Datum,
            speechRep.Wörter
          ),
          NonEmptyList.of("Redner", "Thema", "Datum", "Wörter")
        ).toOption.get
      ),
      Right(speechRep)
    )
  }
  test("Decoding from CSV takes the headers into account") {
    val decoder = CsvRowDecoder[SpeechRep, String]
    val speechRep = SpeechRep("Emacs", "Something", "As if", "Waterfall")
    assertEquals(
      decoder(
        CsvRow(
          NonEmptyList.of(
            speechRep.Wörter,
            speechRep.Datum,
            speechRep.Thema,
            speechRep.Redner
          ),
          NonEmptyList.of("Wörter", "Datum", "Thema", "Redner")
        ).toOption.get
      ),
      Right(speechRep)
    )
  }
  test("Reading speech data from textual representation works") {
    val speechRep = SpeechRep("IDEA", "Blablabla", "1981-08-12", "17")
    val speech = Speech("IDEA", "Blablabla", LocalDate.of(1981, 8, 12), 17)
    assertEquals(Speech.fromRep(speechRep), speech)
  }
  test("Reading speech data trims lines") {
    val speechRep =
      SpeechRep("  VSCode ", "Whatever   ", "2023-12-25", " 1234321")
    val speech =
      Speech("VSCode", "Whatever", LocalDate.of(2023, 12, 25), 1234321)
    assertEquals(Speech.fromRep(speechRep), speech)
  }
}
