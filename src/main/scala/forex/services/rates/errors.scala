package forex.services.rates

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
  }

}
