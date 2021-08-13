package ghyll

// import cats.effect.IO
// import ghyll.json.JsonToken._
// import ghyll.{Decoder}
import ghyll.{TestDecoder, TestEncoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class SemiAutoDerivationSpec extends AnyWordSpec with Matchers with TestDecoder with TestEncoder with Checkers {
  // import ghyll.auto.semi._

  // case class Foo(bar: String)
  // case class WrappedFoo(foo: Foo)

  // case class Response[A](data: A)

  // "deriveDecoder" should {
  //   "derive a decoder" when {
  //     "a case class has only scalar attributes" in {
  //       implicit val decoder: Decoder[Foo] = deriveDecoder

  //       check(
  //         testDecoder(
  //           Foo("baz"),
  //           BeginObject ::
  //             Key("bar") ::
  //             Str("baz") ::
  //             EndObject ::
  //             Nil
  //         )
  //       )
  //     }

  //     "case classes are nested" in {
  //       implicit val fooDecoder: Decoder[Foo] = deriveDecoder
  //       implicit def wrappedDecoder: Decoder[WrappedFoo] = deriveDecoder

  //       check(
  //         testDecoder(
  //           WrappedFoo(Foo("baz")),
  //           BeginObject ::
  //             Key("foo") ::
  //             BeginObject ::
  //             Key("bar") ::
  //             Str("baz") ::
  //             EndObject ::
  //             EndObject ::
  //             Nil
  //         )
  //       )

  //     }

  //     "a case class has generic attributes" in {
  //       implicit val fooDecoder: Decoder[Foo] = deriveDecoder
  //       implicit def responseDecoder[A](implicit d: Decoder[A]): Decoder[Response[A]] = deriveDecoder

  //       check(
  //         testDecoder(
  //           Response(Foo("baz")),
  //           BeginObject ::
  //             Key("data") ::
  //             BeginObject ::
  //             Key("bar") ::
  //             Str("baz") ::
  //             EndObject ::
  //             EndObject ::
  //             Nil
  //         )
  //       )
  //     }
  //   }
  // }

  // "deriveEncoder" should {
  //   "derive an encoder" when {
  //     "a case class has only scalar attributes" in {
  //       implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
  //       check(
  //         testEncoder(Foo("baz"), List(BeginObject, Key("bar"), Str("baz"), EndObject))
  //       )
  //     }

  //     "case classes are nested" in {
  //       implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
  //       implicit val wrappedFooEncoder: Encoder[WrappedFoo] = deriveEncoder[WrappedFoo]

  //       check(
  //         testEncoder(
  //           WrappedFoo(Foo("baz")),
  //           List(BeginObject, Key("foo"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
  //         )
  //       )
  //     }

  //     "a case class has generic attributes" in {
  //       implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
  //       implicit val responseEncoder: Encoder[Response[Foo]] = deriveEncoder[Response[Foo]]

  //       check(
  //         testEncoder(
  //           Response(Foo("baz")),
  //           List(BeginObject, Key("data"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
  //         )
  //       )
  //     }
  //   }
  // }

  // "deriveCodec" should {
  //   "derive an codec" when {
  //     "a case class has only scalar attributes" in {
  //       implicit val fooCodec: Codec[Foo] = deriveCodec

  //       check(
  //         testDecoder(Foo("baz"), List(BeginObject, Key("bar"), Str("baz"), EndObject)) &&
  //           testEncoder(Foo("baz"), List(BeginObject, Key("bar"), Str("baz"), EndObject))
  //       )
  //     }

  //     "case classes are nested" in {
  //       implicit val fooCodec: Codec[Foo] = deriveCodec
  //       implicit val wrappedFooCodec: Codec[WrappedFoo] = deriveCodec

  //       check(
  //         testDecoder(
  //           WrappedFoo(Foo("baz")),
  //           List(BeginObject, Key("foo"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
  //         ) &&
  //           testEncoder(
  //             WrappedFoo(Foo("baz")),
  //             List(BeginObject, Key("foo"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
  //           )
  //       )
  //     }

  //     "a case class has generic attributes" in {
  //       implicit val fooCodec: Codec[Foo] = deriveCodec
  //       implicit val responseCodec: Codec[Response[Foo]] = deriveCodec

  //       check(
  //         testDecoder(
  //           Response(Foo("baz")),
  //           List(BeginObject, Key("data"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
  //         ) &&
  //           testEncoder(
  //             Response(Foo("baz")),
  //             List(BeginObject, Key("data"), BeginObject, Key("bar"), Str("baz"), EndObject, EndObject)
  //           )
  //       )
  //     }
  //   }
  // }

}
