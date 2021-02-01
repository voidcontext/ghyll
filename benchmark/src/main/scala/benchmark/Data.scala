package benchmark

import java.time.LocalDate
import java.util.UUID

import scala.util.Random

import cats.effect.Sync
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

object Data {
  case class PricePoint(date: LocalDate, price: BigDecimal)
  object PricePoint {
    implicit val codec: Codec[PricePoint] = deriveCodec[PricePoint]
  }

  case class Item(name: String, prices: List[PricePoint])
  object Item {
    implicit val codec: Codec[Item] = deriveCodec[Item]
  }

  type DataSet = Map[String, Item]

  object DataSet {
    implicit val codec: Codec[DataSet] = Codec.from(Decoder.decodeMap[String, Item], Encoder.encodeMap[String, Item])
  }

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
