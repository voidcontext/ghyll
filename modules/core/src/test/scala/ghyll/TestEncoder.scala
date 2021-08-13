package ghyll

// import cats.effect.IO
// import cats.effect.unsafe.implicits.global
import ghyll.json.JsonToken
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestEncoder {
  def testEncoder[A](value: A, expected: List[JsonToken])(implicit encoder: Encoder[A]): Prop =
    encoder
      .encode(value)
      .fold(
        e => Prop.falsified :| s"Got an error: $e",
        stream => {
          val actual = stream.toList
          ((actual == expected): Prop) :| s"expected $expected, but got $actual"
        }
      )

  def byteArrayToString(ba: Array[Byte]): String =
    ba.toList.map(_.toInt).toString()
}
