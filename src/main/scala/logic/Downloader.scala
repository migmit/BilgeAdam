package logic

import cats.effect.IO
import cats.syntax.either.catsSyntaxEitherId
import exceptions.DownloadException
import fs2.Stream
import fs2.data.csv.ParseableHeader
import fs2.data.csv.decodeUsingHeaders
import models.Speech
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client

trait Downloader[Url, Item] {

  /** Resolve a URL, producing the stream of items
    *
    * @param url
    *   URL to resolve
    * @return
    *   stream of items
    */
  def getContent(url: Url): Stream[IO, Item]
}

/** Downloader, collecting data for politicians from CSV, downloaded by URL
  *
  * @param client
  *   HTTP client for resolving URLs
  */
class CsvDownloader(client: Client[IO]) extends Downloader[String, Speech] {
  given ParseableHeader[String] = ParseableHeader.instance(_.trim().asRight)
  def getContent(url: String): Stream[IO, Speech] = Uri
    .fromString(url)
    .toOption
    .map(uri =>
      client
        .stream(Request(uri = uri))
        .flatMap(response =>
          if (response.status.isSuccess)
            response.bodyText.through(decodeUsingHeaders[Speech]())
          else Stream.raiseError[IO](new DownloadException)
        )
    )
    .getOrElse(Stream.raiseError(new DownloadException))
}
