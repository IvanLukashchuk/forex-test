package forex.programs.rates

import forex.domain.Currency.Currency

object Protocol {

  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  )

}
