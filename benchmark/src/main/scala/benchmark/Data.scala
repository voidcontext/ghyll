package benchmark

import java.time.LocalDate
import java.util.UUID

import scala.util.Random

import cats.effect.Sync
import ghyll._
import ghyll.auto.semi._
import cats.effect.IO

object Data {
  case class PricePoint(date: LocalDate, price: BigDecimal)
  object PricePoint {
    implicit val codec: Codec[IO, PricePoint] = deriveCodec
  }

  case class Item(name: String, prices: List[PricePoint])
  object Item {
    implicit val codec: Codec[IO, Item] = deriveCodec
  }

  type DataSet = Map[String, Item]

  private val r = Random

  def generate[F[_]: Sync]: F[DataSet] =
    Sync[F].delay {
      Map.from(
        (0 until 1000).map { _ =>
          UUID.randomUUID().toString() ->
            Item(
              name = Random.alphanumeric.take(5 + r.nextInt(10)).mkString,
              prices = (0 until 1000).map(_ => generatePricePoint()).toList
            )
        }
      )
    }

  private def generatePricePoint(): PricePoint =
    PricePoint(
      date = LocalDate.now.minusDays(r.nextInt(10 * 365).toLong),
      price = BigDecimal.valueOf(r.nextDouble())
    )

}
