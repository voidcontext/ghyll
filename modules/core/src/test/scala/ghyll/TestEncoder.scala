package ghyll

import ghyll.json.JsonToken
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestEncoder {
  def testEncoder[A](value: A, expected: List[JsonToken])(implicit encoder: Encoder[A]): Prop =
    testEncoder(value, LazyList.from(expected).map(t => Right(t)))

  def testEncoder[A](value: A, expected: StreamingEncoderResult)(implicit encoder: Encoder[A]): Prop = {
    val actual = encoder.encode(value)
    ((actual == expected): Prop) :| s"expected $expected, but got $actual"
  }

  def byteArrayToString(ba: Array[Byte]): String =
    ba.toList.map(_.toInt).toString()
}
