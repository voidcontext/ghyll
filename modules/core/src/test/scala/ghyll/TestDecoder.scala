package ghyll

// import cats.effect.IO
// import cats.effect.unsafe.implicits.global
import org.scalacheck.Prop
import cats.effect.IO
import ghyll.json.JsonTokenReader.JsonTokenReaderResult

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestDecoder {
  def testDecoder[A](value: A, tokens: List[JsonTokenReaderResult])(implicit decoder: Decoder[IO, A]): Prop = ???

  def testDecoderFailure[A](message: String, tokens: List[JsonTokenReaderResult])(implicit decoder: Decoder[IO, A]): Prop = ???
}
