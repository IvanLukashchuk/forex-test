package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.config.ForexConfig
import forex.domain.Rate
import forex.services.rates.interpreters._
import org.http4s.client.Client

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def service[F[_]: Sync](client: Client[F], forexConfig: ForexConfig): Algebra[F] =
    new OneFrameService[F](
      client,
      forexConfig,
      new AtomicReference[Map[Rate.Pair, Rate]](Map.empty),
      new AtomicReference[Instant](Instant.MIN)
    )
}
