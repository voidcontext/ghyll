package ghyll

//import cats.effect.IO
import ghyll.json.JsonToken
import ghyll.{TestDecoder, TestEncoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import ghyll.json.JsonValue
import ghyll.utils.EitherOps
import ghyll.json.JsonTokenReader.JsonTokenReaderResult
import cats.effect.IO

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
          JsonToken.BeginObject.left ::
            JsonValue.Key("data").right ::
            JsonToken.BeginObject.left ::
            JsonValue.Key("foo").right ::
            JsonToken.BeginObject.left ::
            JsonValue.Key("bar").right ::
            JsonValue.Str("baz").right ::
            JsonToken.EndObject.left ::
            JsonToken.EndObject.left ::
            JsonToken.EndObject.left ::
            Nil
        )
      )
    }

    "derive encoders automatically for case classes" in {
      import ghyll.auto._

      check(
        testEncoder(
          Response(WrappedFoo(Foo("baz"))),
          JsonToken.BeginObject.left ::
            JsonValue.Key("data").right ::
            JsonToken.BeginObject.left ::
            JsonValue.Key("foo").right ::
            JsonToken.BeginObject.left ::
            JsonValue.Key("bar").right ::
            JsonValue.Str("baz").right ::
            JsonToken.EndObject.left ::
            JsonToken.EndObject.left ::
            JsonToken.EndObject.left ::
            Nil
        )
      )
    }

    "derive codecs automatically for case classes" in {
      import ghyll.auto._

      def test[A](value: A, json: List[JsonTokenReaderResult])(implicit c: Codec[IO, A]) = {
        testDecoder(value, json) && testEncoder(value, json)
      }

      check(
        test(
          Response(WrappedFoo(Foo("baz"))),
          JsonToken.BeginObject.left ::
            JsonValue.Key("data").right ::
            JsonToken.BeginObject.left ::
            JsonValue.Key("foo").right ::
            JsonToken.BeginObject.left ::
            JsonValue.Key("bar").right ::
            JsonValue.Str("baz").right ::
            JsonToken.EndObject.left ::
            JsonToken.EndObject.left ::
            JsonToken.EndObject.left ::
            Nil
        )
      )
    }
  }
}
