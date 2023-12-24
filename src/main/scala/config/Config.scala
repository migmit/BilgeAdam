package config

import org.http4s.Uri
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._
import pureconfig.module.http4s._

import scala.concurrent.duration.Duration

/** SSL configuration
  *
  * @param keyStore
  *   resource file, p12
  * @param keyStorePassword
  *   password for that keyStore
  */
case class TLSConfig(keyStore: String, keyStorePassword: String)
    derives ConfigReader

/** Configuration for the HTTP server
  *
  * @param host
  *   host to bind the server to
  * @param port
  *   port to listen at
  * @param tls
  *   key store parameters for HTTPS
  */
case class ServerConfig(host: Uri.Host, port: Int, tls: Option[TLSConfig])
    derives ConfigReader

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
case class Http4sConfig(server: List[ServerConfig], client: ClientConfig)
    derives ConfigReader
