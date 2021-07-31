package ghyll

import ghyll.json.JsonToken
import ghyll.{/*Codec,*/ TestDecoder, TestEncoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class AutoDerivationSpec extends AnyWordSpec with Matchers with TestEncoder with TestDecoder with Checkers {
  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "auto" should {
    "derive decoders automatically for case classes" in {
      import ghyll.auto._

      check(
        testDecoder(
          Response(WrappedFoo(Foo("baz"))),
          JsonToken.BeginObject ::
            JsonToken.Key("data") ::
            JsonToken.BeginObject ::
            JsonToken.Key("foo") ::
            JsonToken.BeginObject ::
            JsonToken.Key("bar") ::
            JsonToken.Str("baz") ::
            JsonToken.EndObject ::
            JsonToken.EndObject ::
            JsonToken.EndObject ::
            Nil
        )
      )
    }

    "derive encoders automatically for case classes" in {
      import ghyll.auto._

      check(
        testEncoder(
          Response(WrappedFoo(Foo("baz"))),
          JsonToken.BeginObject ::
            JsonToken.Key("data") ::
            JsonToken.BeginObject ::
            JsonToken.Key("foo") ::
            JsonToken.BeginObject ::
            JsonToken.Key("bar") ::
            JsonToken.Str("baz") ::
            JsonToken.EndObject ::
            JsonToken.EndObject ::
            JsonToken.EndObject ::
            Nil
        )
      )
    }

    // "derive codecs automatically for case classes" in {
    //   def test[A: Codec](value: A, json: String) = {
    //     testDecoder(value, json)
    //     testEncoder(value, json)
    //   }

    //   import ghyll.auto._
    //   test(Response(WrappedFoo(Foo("baz"))), """{"data": {"foo": {"bar": "baz"}}}""")
    // }
  }
}
