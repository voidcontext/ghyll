package ghyll

import org.scalacheck.Prop
import cats.effect.IO
import ghyll.json.JsonTokenReader.JsonTokenReaderResult

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestEncoder {
  def testEncoder[A](value: A, expected: List[JsonTokenReaderResult])(implicit encoder: Encoder[IO, A]): Prop = ???

  def testEncoder[A](value: A, expected: StreamingEncoderResult[IO])(implicit encoder: Encoder[IO, A]): Prop = ???

  def byteArrayToString(ba: Array[Byte]): String =  ???
}
