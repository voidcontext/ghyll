package ghyll.auto.semi

import ghyll.{Codec, Decoder, Encoder, TestDecoder, TestEncoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DerivationSpec extends AnyWordSpec with Matchers with TestDecoder with TestEncoder {
  import ghyll.auto.semi._

  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "deriveDecoder" should {
    "derive a decoder" when {
      "a case class has only scalar attributes" in {
        implicit val decoder: Decoder[Foo] = deriveDecoder[Foo]

        testDecoder(Foo("baz"), """{"bar": "baz"}""")
      }

      "case classes are nested" in {
        implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
        implicit def responseDecoder[A: Decoder]: Decoder[Response[A]] = deriveDecoder

        testDecoder(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")

      }

      "a case class has generic attributes" in {
        implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
        implicit def responseDecoder[A: Decoder]: Decoder[Response[A]] = deriveDecoder

        testDecoder(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")
      }
    }
  }

  "deriveEncoder" should {
    "derive an encoder" when {
      "a case class has only scalar attributes" in {
        implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
        testEncoder(Foo("baz"), """{"bar": "baz"}""")
      }

      "case classes are nested" in {
        implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
        implicit val wrappedFooEncoder: Encoder[WrappedFoo] = deriveEncoder[WrappedFoo]

        testEncoder(WrappedFoo(Foo("baz")), """{"foo": {"bar": "baz"}}""")
      }

      "a case class has generic attributes" in {
        implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
        implicit val responseEncoder: Encoder[Response[Foo]] = deriveEncoder[Response[Foo]]

        testEncoder(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")
      }
    }
  }

  "deriveCodec" should {
    "derive an codec" when {
      "a case class has only scalar attributes" in {
        implicit val fooCodec: Codec[Foo] = deriveCodec[Foo]
        testDecoder(Foo("baz"), """{"bar": "baz"}""")
        testEncoder(Foo("baz"), """{"bar": "baz"}""")
      }

      "case classes are nested" in {
        implicit val fooCodec: Codec[Foo] = deriveCodec[Foo]
        implicit val wrappedFooCodec: Codec[WrappedFoo] = deriveCodec[WrappedFoo]

        testDecoder(WrappedFoo(Foo("baz")), """{"foo": {"bar": "baz"}}""")
        testEncoder(WrappedFoo(Foo("baz")), """{"foo": {"bar": "baz"}}""")
      }

      "a case class has generic attributes" in {
        implicit val fooCodec: Codec[Foo] = deriveCodec[Foo]
        implicit val responseCodec: Codec[Response[Foo]] = deriveCodec[Response[Foo]]

        testDecoder(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")
        testEncoder(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")
      }
    }
  }

}
