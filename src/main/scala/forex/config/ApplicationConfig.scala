package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    forexConfig: ForexConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class ForexConfig(
    host: String,
    port: Int,
    ttl: FiniteDuration,
    path: String,
    token: String
)
