package ghyll

import cats.effect.IO
import ghyll.json.JsonToken
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestEncoder {
  def testEncoder[A](value: A, expected: List[JsonToken])(implicit encoder: Encoder[IO, A]): Prop = ???

  def testEncoder[A](value: A, expected: StreamingEncoderResult[IO])(implicit encoder: Encoder[IO, A]): Prop = ???

  def byteArrayToString(ba: Array[Byte]): String = ???
}
