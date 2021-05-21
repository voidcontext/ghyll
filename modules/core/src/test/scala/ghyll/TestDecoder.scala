package ghyll

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.Stream
import ghyll.json.JsonToken
import org.scalacheck.Prop
// import ghyll.Utils.createReader

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestDecoder {
  def testDecoder[A](value: A, json: Stream[IO, JsonToken])(implicit decoder: Decoder[IO, A]): Prop =
    decoder
      .decode(json)
      .compile
      .lastOrError
      .map { decoded =>
        val unwrapped = decoded.map(_._1)
        (unwrapped == Right(value): Prop) :| s"expected: Right($value), got $unwrapped"
      }
      .unsafeRunSync()

  def testDecoderFailure[A](message: String, json: Stream[IO, JsonToken])(implicit decoder: Decoder[IO, A]): Prop =
    decoder
      .decode(json)
      .compile
      .lastOrError
      .map { decoded =>
        val unwrapped = decoded.map(_._1)
        (unwrapped == Left(
          StreamingDecodingFailure(message)
        ): Prop) :| s"expected: Left(StreamingDecodingFailure($message)), got $unwrapped"
      }
      .unsafeRunSync()

  // def testDecoder[A](value: A, json: String)(implicit decoder: Decoder[A]): Prop =
  //   createReader(json).use { reader =>
  //     IO.delay(decoder.decode(reader))
  //   }
  //     .map(decoded => (decoded == Right(value): Prop) :| s"expected: Right($value), got $decoded")
  //     .unsafeRunSync()

}
