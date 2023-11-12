package forex.domain

import cats.effect.IO
import forex.domain.Currency.Currency
import io.circe.Decoder
import org.http4s.{EntityDecoder, circe}

import java.time.OffsetDateTime

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  implicit val pairOrdering: Ordering[Pair] = Ordering.by(pair => (pair.from.id, pair.to.id))

  implicit val rateDecoder: Decoder[Rate] = Decoder.instance { cursor =>
    for {
      from <- cursor.get[Currency]("from")
      to <- cursor.get[Currency]("to")
      price <- cursor.get[Integer]("price")
      timestamp <- cursor.get[OffsetDateTime]("timestamp")
    } yield Rate(Pair(from, to), Price(price), Timestamp(timestamp))
  }

  implicit val rateEntityDecoder: EntityDecoder[IO, Rate] = circe.jsonOf[IO, Rate]
}
