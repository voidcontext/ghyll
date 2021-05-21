package ghyll.auto

import ghyll.{/*Codec,*/ TestDecoder, TestEncoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AutoDerivationSpec extends AnyWordSpec with Matchers with TestEncoder with TestDecoder {
  // case class Foo(bar: String)
  // case class WrappedFoo(foo: Foo)

  // case class Response[A](data: A)

  // "auto" should {
  //   "derive decoders automatically for case classes" in {
  //     import ghyll.auto._
  //     testDecoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
  //   }

  //   "derive encoders automatically for case classes" in {
  //     import ghyll.auto._
  //     testEncoder(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
  //   }

  //   "derive codecs automatically for case classes" in {
  //     def test[A: Codec](value: A, json: String) = {
  //       testDecoder(value, json)
  //       testEncoder(value, json)
  //     }

  //     import ghyll.auto._
  //     test(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
  //   }
  // }
}
