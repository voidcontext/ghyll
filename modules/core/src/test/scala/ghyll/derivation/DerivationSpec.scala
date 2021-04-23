package ghyll.derivation

import ghyll.{Decoder, Encoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DerivationSpec extends AnyWordSpec with Matchers with Derivation {
  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "deriveDecoder" should {
    "derive a decoder" when {
      "a case class has only scalar attributes" in {
        deriveDecoder[Foo] should be(a[Decoder[_]])
      }

      "case classes are nested" in {
        implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
        deriveDecoder[WrappedFoo] should be(a[Decoder[_]])

      }

      "a case class has generic attributes" in {
        implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
        deriveDecoder[Response[Foo]] should be(a[Decoder[_]])
      }
    }
  }

  "deriveEncoder" should {
    "derive an encoder" when {
      "a case class has only scalar attributes" in {
        deriveEncoder[Foo] should be(a[Encoder[_]])
      }

      "case classes are nested" in {
        implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
        deriveEncoder[WrappedFoo] should be(a[Encoder[_]])

      }

      "a case class has generic attributes" in {
        implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
        deriveEncoder[Response[Foo]] should be(a[Encoder[_]])
      }
    }
  }

}
