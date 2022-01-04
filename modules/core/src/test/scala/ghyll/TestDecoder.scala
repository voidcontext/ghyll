package ghyll

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ghyll.json.{JsonToken, TestJsonTokenReader}
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestDecoder {
  def testDecoder[A](value: A, tokens: List[JsonToken])(implicit decoder: Decoder[IO, A]): Prop =
    (for {
      reader <- TestJsonTokenReader.withTokens(tokens)
      result <- decoder.decode(reader)
    } yield result.fold(
      err => (false: Prop) :| s"expected Right value, but got Left($err)",
      decoded => (decoded == value: Prop) :| s"expected: Righ($value), got Right($decoded)"
    )).unsafeRunSync()

  def testDecoderFailure[A](message: String, tokens: List[JsonToken])(implicit decoder: Decoder[IO, A]): Prop =
    (for {
      reader <- TestJsonTokenReader.withTokens(tokens)
      result <- decoder.decode(reader)
    } yield (result == Left(
      StreamingDecodingFailure(message)
    ): Prop) :| s"expected: Left(StreamingDecodingFailure($message)), got $result").unsafeRunSync()

}
