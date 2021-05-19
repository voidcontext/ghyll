package ghyll

import cats.effect.IO
import ghyll.Utils.createReader
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestDecoder {
  def testDecoder[A](value: A, json: String)(implicit decoder: Decoder[A]): Prop =
    createReader(json).use { reader =>
      IO.delay(decoder.decode(reader))
    }
      .map(decoded => (decoded == Right(value): Prop) :| s"expected: Right($value), got $decoded")
      .unsafeRunSync()

}
