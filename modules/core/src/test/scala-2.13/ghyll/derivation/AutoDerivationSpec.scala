package ghyll.derivation

import cats.effect.IO
import ghyll.Utils.createReader
import ghyll.{Decoder, TestEncoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AutoDerivationSpec extends AnyWordSpec with Matchers with TestEncoder {
  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "auto" should {
    "derive decoders automatically for case classes" in {
      import ghyll.derivation.auto._
      checkDecoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
    }

    "derive encoders automatically for case classes" in {
      import ghyll.derivation.auto._
      testEncoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
    }
  }

  def checkDecoder[T](expected: T, json: String)(implicit decoder: Decoder[T]) = {
    createReader(json)
      .use(reader => IO.delay(decoder.decode(reader)))
      .map(_ should be(Right(expected)))
      .unsafeRunSync()
  }

}
