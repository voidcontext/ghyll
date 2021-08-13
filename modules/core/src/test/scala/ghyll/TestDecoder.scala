package ghyll

// import cats.effect.IO
// import cats.effect.unsafe.implicits.global
import ghyll.json.JsonToken
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestDecoder extends TestTokenStream {
  def testDecoder[A](value: A, tokens: List[JsonToken])(implicit decoder: Decoder[A]): Prop =
    decoder.decode(tokenStream(tokens)) match {
      case Right(result -> remaining) =>
        (result == value: Prop) :| s"expected: Right($value), got $result" &&
        (remaining == Nil: Prop) :| s"Stream wasn't fully consumed, reamining: $remaining"
      case Left(err)               => (false: Prop) :| s"expected Right value, but got Left($err)"
    }


  def testDecoderFailure[A](message: String, tokens: List[JsonToken])(implicit decoder: Decoder[A]): Prop = {
    val decoded = decoder
      .decode(tokenStream(tokens))

    val unwrapped = decoded.map(_._1)
    (unwrapped == Left(
      StreamingDecodingFailure(message)
    ): Prop) :| s"expected: Left(StreamingDecodingFailure($message)), got $unwrapped"
  }
}
