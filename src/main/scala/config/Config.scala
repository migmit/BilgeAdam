package config

import org.http4s.Uri
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._
import pureconfig.module.http4s._

import scala.concurrent.duration.Duration

case class ServerConfig(host: Uri.Host, port: Int) derives ConfigReader
case class ClientConfig(
    timeout: Option[Duration],
    idleConnectionTime: Option[Duration]
) derives ConfigReader
case class Http4sConfig(server: ServerConfig, client: ClientConfig)
    derives ConfigReader
