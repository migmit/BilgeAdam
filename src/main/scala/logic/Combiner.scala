package logic

import cats.effect.IO
import cats.syntax.parallel.catsSyntaxParallelFoldMapA
import models.Evaluation

/** Main business logic. Collect data from individual sources using the provided
  * `downloader`, then combine those data.
  *
  * @param downloader
  *   specific way to resolve URLs and collect the data
  */
class Combiner(downloader: GenericDownloader) {

  /** Resolve given URLs and combine all collected data
    *
    * @param urls
    *   data sources
    * @return
    *   result of collecting and processing all the data
    */
  def combine(urls: Seq[String]): IO[Evaluation] =
    urls.parFoldMapA(downloader.handleUrl).map(Evaluation.fromStats)
}
