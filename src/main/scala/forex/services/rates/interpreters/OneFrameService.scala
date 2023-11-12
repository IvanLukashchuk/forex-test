package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import forex.config.ForexConfig
import forex.domain.{Currency, Rate}
import forex.services.rates.Algebra
import forex.services.rates.errors.Error
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.headers.Authorization

import java.time.{Duration, Instant}
import java.util.concurrent.atomic.AtomicReference

class OneFrameService[F[_]: Sync](client: Client[F],
                                              forexConfig: ForexConfig,
                                              rates: AtomicReference[Map[Rate.Pair, Rate]],
                                              lastUpdated: AtomicReference[Instant])
    extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Either[Error, Rate]] =
    for {
      stale <- Sync[F].delay(isStale(lastUpdated.get()))
      _ <- if (stale) updateRates() else Sync[F].unit
      rate <- Sync[F].delay(rates.get.get(pair).toRight(Error.OneFrameLookupFailed("Rate not available")))
    } yield rate

  private def updateRates(): F[Unit] = synchronized { // TODO allow other threads to read old rates while update is in progress
    fetchRates(Currency.allPairs()).flatMap {
      case Right(freshRates) =>
        Sync[F].delay {
          val updatedRates = freshRates.map(rate => rate.pair -> rate).toMap
          rates.set(updatedRates)
          lastUpdated.set(Instant.now())
        }
      case Left(error) => Sync[F].raiseError[Unit](error)
    }
  }

  private def isStale(timestamp: Instant): Boolean =
    Duration.between(timestamp, Instant.now()).toMinutes >= forexConfig.ttl.toMinutes

  private def fetchRates(pairs: Set[Rate.Pair]): F[Either[Error, List[Rate]]] = {
    val queryParams = pairs.map(pair => s"pair=${pair.from}${pair.to}").mkString("&")

    val request = Request[F](
      method = Method.GET,
      uri = Uri
        .fromString(s"http://${forexConfig.host}:${forexConfig.port}/${forexConfig.path}?$queryParams")
        .getOrElse(Uri()),
      headers = Headers.of(Authorization(Credentials.Token(AuthScheme.Bearer, forexConfig.token)))
    )

    client.expect[List[Rate]](request).attempt.map {
      case Right(rates) => Right(rates)
      case Left(_)      => Left(Error.OneFrameLookupFailed("Failed to fetch rates from One Frame"))
    }
  }

}
