package ghyll

import cats.effect.IO
import ghyll.json.JsonToken._
import ghyll.{Decoder, TestDecoder, TestEncoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SemiAutoDerivationSpec extends AnyWordSpec with Matchers with TestDecoder with TestEncoder {
  import ghyll.auto.semi._

  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "deriveDecoder" should {
    "derive a decoder" when {
      "a case class has only scalar attributes" in {
        implicit val decoder: Decoder[IO, Foo] = deriveDecoder

        testDecoder(
          Foo("baz"),
          BeginObject ::
            Key("bar") ::
            Str("baz") ::
            EndObject ::
            Nil
        )
      }

      "case classes are nested" in {
        implicit val fooDecoder: Decoder[IO, Foo] = deriveDecoder
        implicit def wrappedDecoder: Decoder[IO, WrappedFoo] = deriveDecoder

        testDecoder(
          WrappedFoo(Foo("baz")),
          BeginObject ::
            Key("foo") ::
            BeginObject ::
            Key("bar") ::
            Str("baz") ::
            EndObject ::
            EndObject ::
            Nil
        )

      }

      "a case class has generic attributes" in {
        implicit val fooDecoder: Decoder[IO, Foo] = deriveDecoder
        implicit def responseDecoder[A](implicit d: Decoder[IO, A]): Decoder[IO, Response[A]] = deriveDecoder

        testDecoder(
          Response(Foo("baz")),
          BeginObject ::
            Key("data") ::
            BeginObject ::
            Key("bar") ::
            Str("baz") ::
            EndObject ::
            EndObject ::
            Nil
        )
      }
    }
  }

  "deriveEncoder" should {
    "derive an encoder" when {
      "a case class has only scalar attributes" in {
        implicit val fooEncoder: Encoder[IO, Foo] = deriveEncoder[IO, Foo]
        testEncoder(Foo("baz"), List(BeginObject, Key("bar"), Str("baz"), EndObject))
      }

      "case classes are nested" in {
        implicit val fooEncoder: Encoder[IO, Foo] = deriveEncoder[IO, Foo]
        implicit val wrappedFooEncoder: Encoder[IO, WrappedFoo] = deriveEncoder[IO, WrappedFoo]

        testEncoder(WrappedFoo(Foo("baz")), 
          List(BeginObject, Key("foo"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
        )
      }

      "a case class has generic attributes" in {
        implicit val fooEncoder: Encoder[IO, Foo] = deriveEncoder[IO, Foo]
        implicit val responseEncoder: Encoder[IO, Response[Foo]] = deriveEncoder[IO, Response[Foo]]

        testEncoder(Response(Foo("baz")), 
          List(BeginObject, Key("data"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
        )
      }
    }
  }

  // "deriveCodec" should {
  //   "derive an codec" when {
  //     "a case class has only scalar attributes" in {
  //       implicit val fooCodec: Codec[Foo] = deriveCodec[Foo]
  //       testDecoder(Foo("baz"), """{"bar": "baz"}""")
  //       testEncoder(Foo("baz"), """{"bar": "baz"}""")
  //     }

  //     "case classes are nested" in {
  //       implicit val fooCodec: Codec[Foo] = deriveCodec[Foo]
  //       implicit val wrappedFooCodec: Codec[WrappedFoo] = deriveCodec[WrappedFoo]

  //       testDecoder(WrappedFoo(Foo("baz")), """{"foo": {"bar": "baz"}}""")
  //       testEncoder(WrappedFoo(Foo("baz")), """{"foo": {"bar": "baz"}}""")
  //     }

  //     "a case class has generic attributes" in {
  //       implicit val fooCodec: Codec[Foo] = deriveCodec[Foo]
  //       implicit val responseCodec: Codec[Response[Foo]] = deriveCodec[Response[Foo]]

  //       testDecoder(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")
  //       testEncoder(Response(Foo("baz")), """{"data": {"bar": "baz"}}""")
  //     }
  //   }
  // }

}
