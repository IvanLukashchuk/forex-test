package forex.domain

import cats.Show
import forex.domain.Rate.Pair
import io.circe.Decoder

object Currency extends Enumeration {
  type Currency = Value

  val AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD = Value

  implicit val show: Show[Currency] = Show.show(c => c.toString)

  def fromString(s: String): Option[Currency] = Currency.values.find(_.toString == s.toUpperCase)

  implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeEnumeration(Currency)

  def allPairs(): Set[Pair] =
    (for {
      a <- Currency.values
      b <- Currency.values if a != b
    } yield Pair(a, b)).toSet

}
