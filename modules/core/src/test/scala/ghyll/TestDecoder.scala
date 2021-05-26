package ghyll

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ghyll.json.JsonToken
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestDecoder extends TestTokenStream {
  def testDecoder[A](value: A, tokens: List[JsonToken])(implicit decoder: Decoder[IO, A]): Prop =
    withTokenStream(tokens) { stream =>
      decoder
        .decode(stream)
        .compile
        .lastOrError
        .flatMap {
          case Right(result -> stream) =>
            stream.compile.toList.map { remaining =>
              (result == value: Prop) :| s"expected: Right($value), got $result" &&
              (remaining == Nil: Prop) :| s"Stream wasn't fully consumed, reamining: $remaining"
            }
          case Left(err)               => IO.pure((false: Prop) :| s"expected Right value, but got Left($err)")
        }
    }

  def testDecoderFailure[A](message: String, json: TokenStream[IO])(implicit decoder: Decoder[IO, A]): Prop =
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
