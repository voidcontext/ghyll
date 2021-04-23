package ghyll.auto.semi

import cats.effect.IO
import ghyll._
import ghyll.Utils.createReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DerivesSpec extends AnyWordSpec with Matchers with TestDecoder with TestEncoder {
  "deriving Decoder" should {
    "derive a valid decoder" when {
      "derive decoders automatically for case classes" in {
        case class Foo(bar: String) derives Decoder
        case class WrappedFoo(foo: Foo) derives Decoder

        case class Response[A](data: A) derives Decoder

        testDecoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
      }

      "derive encoders automatically for case classes" in {
        case class Foo(bar: String) derives Encoder
        case class WrappedFoo(foo: Foo) derives Encoder

        case class Response[A](data: A) derives Encoder
        testEncoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
      }

      "derive codecs automatically for case classes" in {
        case class Foo(bar: String) derives Codec
        case class WrappedFoo(foo: Foo) derives Codec

        case class Response[A](data: A) derives Codec

        def test[A: Codec](value: A, json: String) = {
          testDecoder(value, json)
          testEncoder(value, json)
        }

        test(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
      }

    }
  }
}
