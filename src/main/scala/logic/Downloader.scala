package logic

import cats.effect.IO
import exceptions.DownloadException
import fs2.Stream
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client

class Downloader(client: Client[IO]) extends GenericDownloader {
  def getContent(url: String): Stream[IO, String] = client
    .stream(Request(uri = Uri.fromString(url).toOption.get))
    .flatMap(response =>
      if (response.status.isSuccess) response.bodyText
      else Stream.raiseError[IO](new DownloadException)
    )
}
