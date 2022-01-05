package ghyll

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ghyll.json.{JsonToken, TestJsonTokenWriter}
import org.scalacheck.Prop

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
trait TestEncoder {
  def testEncoder[A](value: A, expected: List[JsonToken])(implicit encoder: Encoder[IO, A]): Prop =
    (
      for {
        writer  <- TestJsonTokenWriter.apply
        _       <- encoder.encode(value, writer)
        written <- writer.written
      } yield ((written == expected): Prop) :| s"Expected $expected, but got $written"
    ).unsafeRunSync()

  def testEncoder[A](value: A, expected: StreamingEncoderResult[IO])(implicit encoder: Encoder[IO, A]): Prop = ???

  def byteArrayToString(ba: Array[Byte]): String = ???
}
