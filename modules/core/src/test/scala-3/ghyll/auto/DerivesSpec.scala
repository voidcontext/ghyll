package ghyll.auto

import cats.effect.IO
import ghyll._
import ghyll.json.JsonToken._
import ghyll.json.JsonToken
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers


class DerivesSpec extends AnyWordSpec with TestDecoder with TestEncoder with Checkers {
  // Workaround for the bugs:
  // https://github.com/scalatest/scalatest/issues/2030
  // https://github.com/lampepfl/dotty/issues/12508
  object withDecoder {
    case class Foo(bar: String) derives DecoderIO
    case class WrappedFoo(foo: Foo) derives DecoderIO

    case class Response[A](data: A) derives DecoderIO
  }

  object withEncoder {
    case class Foo(bar: String) derives EncoderIO
    case class WrappedFoo(foo: Foo) derives EncoderIO

    case class Response[A](data: A) derives EncoderIO
  }

  object withCodec {
    case class Foo(bar: String) derives CodecIO
    case class WrappedFoo(foo: Foo) derives CodecIO

    case class Response[A](data: A) derives CodecIO

  }

  val jsonRepr = 
    BeginObject ::
      Key("data") ::
      BeginObject ::
      Key("foo") ::
      BeginObject ::
      Key("bar") ::
      Str("baz") ::
      EndObject ::
      EndObject ::
      EndObject ::
      Nil


  "deriving Decoder" should {
    "derive a valid decoder" when {
      "derive decoders automatically for case classes" in {
        import withDecoder._

        check(
          testDecoder(
            Response(WrappedFoo(Foo("baz"))),
            jsonRepr
          )
        )
      }

      "derive encoders automatically for case classes" in {
        import withEncoder._

        check(
          testEncoder(
            Response(WrappedFoo(Foo("baz"))),
            jsonRepr
          )
        )
      }

      "derive codecs automatically for case classes" in {
        import withCodec._

        def test[A: CodecIO](value: A, json: List[JsonToken]) = {
          testDecoder(value, json) && testEncoder(value, json)
        }

        check(
          test(
            Response(WrappedFoo(Foo("baz"))),
            jsonRepr
          )
        )
      }
    }
  }
}
