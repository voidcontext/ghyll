package ghyll.derivation

import cats.effect.IO
import ghyll.Decoder
import ghyll.Utils.createReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DerivationSpec extends AnyWordSpec with Matchers with Derivation {
  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "deriveDecoder" should {
    "derive a valid decoder" when {
      "a case class has only scalar attributes" in {
        implicit val decoder: Decoder[Foo] = deriveDecoder[Foo]

        check(Foo("baz"), """{"bar": "baz"}""")
      }

      "case classes are nested" in {
        implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
        implicit val wrappedDecoder: Decoder[WrappedFoo] = deriveDecoder[WrappedFoo]

        check(WrappedFoo(Foo("baz")), """{"foo": {"bar": "baz"}}""")
      }

      "a case class has generic attributes" in {
        implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
        implicit def responseDecoder[A: Decoder]: Decoder[Response[A]] = deriveDecoder

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
