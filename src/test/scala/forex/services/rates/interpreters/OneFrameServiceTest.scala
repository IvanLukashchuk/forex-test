package forex.services.rates.interpreters

import cats.effect.IO
import forex.config.ForexConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import org.http4s.{EntityDecoder, Request}
import org.http4s.client.Client
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.DurationInt

class OneFrameServiceTest extends AnyFunSuite with MockitoSugar with Matchers {

  val mockClient: Client[IO] = mock[Client[IO]]
  val mockForexConfig: ForexConfig = ForexConfig("host", 80, 5.minutes, "path", "token")

  // Mock data
  val testPair: Rate.Pair = Rate.Pair(Currency.EUR, Currency.USD)
  val testRate: Rate = Rate(testPair, Price(1.2), Timestamp.now)


  test("get should retrieve a rate successfully if not stale") {
    val ratesMap = new AtomicReference(Map(testPair -> testRate))
    val lastUpdated = new AtomicReference(Instant.now.minusSeconds(60 * 4)) // 4 minutes ago
    val serviceWithRates = new OneFrameService[IO](mockClient, mockForexConfig, ratesMap, lastUpdated)

    val result = serviceWithRates.get(testPair).unsafeRunSync()

    assert(result.isRight)
    assert(result.contains(testRate))
  }

  test("get should update rates if data is stale") {
    val ratesMap = new AtomicReference(Map(testPair -> testRate))
    val lastUpdated = new AtomicReference(Instant.now.minusSeconds(60 * 6)) // 6 minutes ago
    val serviceWithRates = new OneFrameService[IO](mockClient, mockForexConfig, ratesMap, lastUpdated)

    when(mockClient.expect[List[Rate]](any[Request[IO]])(any[EntityDecoder[IO, List[Rate]]])).thenReturn(IO.pure(List(testRate)))

    val result = serviceWithRates.get(testPair).unsafeRunSync()

    assert(result.isRight)
    assert(ratesMap.get().contains(testPair))
  }

  test("get should return an error if rate is not available") {
    val ratesMap = new AtomicReference(Map[Rate.Pair, Rate]())
    val lastUpdated = new AtomicReference(Instant.now.minusSeconds(60 * 4)) // 4 minutes ago
    val serviceWithRates = new OneFrameService[IO](mockClient, mockForexConfig, ratesMap, lastUpdated)

    val result = serviceWithRates.get(testPair).unsafeRunSync()

    assert(result.isLeft)
  }

}