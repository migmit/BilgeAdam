package exceptions

import fs2.data.csv.CsvException

import java.time.format.DateTimeParseException

/** Exception thrown while processing one URL
  *
  * @param url
  *   URL being processed
  * @param reason
  *   underlying exception
  */
class URLException[Url](url: Url, reason: Throwable) extends Exception(reason) {
  override def getMessage(): String = {
    val header = reason match {
      case _: DownloadException      => "Download error"
      case _: CsvException           => "Decode error"
      case _: NumberFormatException  => "Number parsing error"
      case _: DateTimeParseException => "Date parsing error"
      case _                         => "Unexpected error"
    }
    s"$header ($url): ${reason.getMessage()}"
  }
}
