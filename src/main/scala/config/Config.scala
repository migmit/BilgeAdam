package config

import org.http4s.Uri
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._
import pureconfig.module.http4s._

import scala.concurrent.duration.Duration

/** Configuration for the HTTP server
  *
  * @param host
  *   host to bind the server to
  * @param port
  *   port to listen at
  */
case class ServerConfig(host: Uri.Host, port: Int) derives ConfigReader

/** Configuration for the HTTP client
  *
  * @param timeout
  *   header receive timeout
  * @param idleConnectionTime
  *   idle timeout
  */
case class ClientConfig(
    timeout: Option[Duration],
    idleConnectionTime: Option[Duration]
) derives ConfigReader

/** Overall configuration for http4s
  *
  * @param server
  *   configuration for the HTTP server
  * @param client
  *   conviguration for the HTTP client
  */
case class Http4sConfig(server: ServerConfig, client: ClientConfig)
    derives ConfigReader
