package ghyll.derivation

import cats.effect.IO
import ghyll.Decoder
import ghyll.Utils.createReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AutoDerivationSpec extends AnyWordSpec with Matchers {
  // Todo change this so that Decoder can be derived directly
  case class Foo(bar: String) derives DerivedDecoder
  case class WrappedFoo(foo: Foo) derives DerivedDecoder

  case class Response[A](data: A) derives DerivedDecoder

  "deriving Decoder" should {
    "derive a valid decoder" when {
      "a case class has only scalar attributes" in {
        check(Foo("baz"), """{"bar": "baz"}""")
      }

      "case classes are nested" in {
        check(WrappedFoo(Foo("baz")), """{"foo": {"bar": "baz"}}""")
      }

      "a case class has generic attributes" in {
        check(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")
      }
    }
  }

  def check[T](expected: T, json: String)(implicit decoder: Decoder[T]) = {
    createReader(json)
      .use(reader => IO.delay(decoder.decode(reader)))
      .map(_ should be(Right(expected)))
      .unsafeRunSync()

  }
}
