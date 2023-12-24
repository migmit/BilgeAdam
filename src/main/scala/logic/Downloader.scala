package logic

import cats.effect.IO
import exceptions.DownloadException
import fs2.Stream
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client

/** Downloader, collecting data for politicians from CSV, downloaded by URL
  *
  * @param client
  *   HTTP client for resolving URLs
  */
class Downloader(client: Client[IO]) extends GenericDownloader {
  def getContent(url: String): Stream[IO, String] =
    Uri
      .fromString(url)
      .toOption
      .map(uri =>
        client
          .stream(Request(uri = uri))
          .flatMap(response =>
            if (response.status.isSuccess) response.bodyText
            else Stream.raiseError[IO](new DownloadException)
          )
      )
      .getOrElse(Stream.raiseError(new DownloadException))
}
