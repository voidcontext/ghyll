package ghyll.derivation

import cats.effect.IO
import ghyll.Decoder
import ghyll.Utils.createReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AutoDerivationSpec extends AnyWordSpec with Matchers {
  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "auto" should {
    "derive decoders automatically for case classes" in {
      import ghyll.derivation.auto._
      check(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
    }
  }

  def check[T](expected: T, json: String)(implicit decoder: Decoder[T]) = {
    createReader(json)
      .use(reader => IO.delay(decoder.decode(reader)))
      .map(_ should be(Right(expected)))
      .unsafeRunSync()

  }

}
