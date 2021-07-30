package ghyll.auto

// import cats.effect.IO
import ghyll._
// import ghyll.Utils.createReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DerivesSpec extends AnyWordSpec with Matchers with TestDecoder with TestEncoder {
  // // Workaround for the bugs:
  // // https://github.com/scalatest/scalatest/issues/2030
  // // https://github.com/lampepfl/dotty/issues/12508
  // object withDecoder {
  //   case class Foo(bar: String) derives Decoder
  //   case class WrappedFoo(foo: Foo) derives Decoder

  //   case class Response[A](data: A) derives Decoder
  // }

  // object withEncoder {
  //   case class Foo(bar: String) derives Encoder
  //   case class WrappedFoo(foo: Foo) derives Encoder

  //   case class Response[A](data: A) derives Encoder
  // }

  // object withCodec {
  //   case class Foo(bar: String) derives Codec
  //   case class WrappedFoo(foo: Foo) derives Codec

  //   case class Response[A](data: A) derives Codec

  // }

  // "deriving Decoder" should {
  //   "derive a valid decoder" when {
  //     "derive decoders automatically for case classes" in {
  //       import withDecoder._
  //       testDecoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
  //     }

  //     "derive encoders automatically for case classes" in {
  //       import withEncoder._
  //       testEncoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
  //     }

  //     "derive codecs automatically for case classes" in {
  //       import withCodec._
  //       def test[A: Codec](value: A, json: String) = {
  //         testDecoder(value, json)
  //         testEncoder(value, json)
  //       }

  //       test(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
  //     }

  //   }
  // }
}
