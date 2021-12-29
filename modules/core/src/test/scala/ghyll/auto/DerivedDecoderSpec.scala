package ghyll.auto

// import fs2.Stream
import ghyll.TestDecoder
import ghyll.json.{JsonToken, JsonValue}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import ghyll.utils.EitherOps

class DerivedDecoderSpec extends AnyWordSpec with Matchers with TestDecoder with Checkers {
  case class Foo(bar: String, baz: Int)
  case class FooOption(bar: Option[String], baz: Int)

  "DerivedDecoder.decode" should {
    "decode case classes" when {

      "there are only scalar values in the case class" in {
        check(
          testDecoder(
            Foo("foobar", 42),
            JsonToken.BeginObject.left ::
              JsonValue.Key("bar").right ::
              JsonValue.Str("foobar").right ::
              JsonValue.Key("baz").right ::
              JsonValue.Number(42).right ::
              JsonToken.EndObject.left ::
              Nil
          )
        )
      }

      "field order is different" in {
        check(
          testDecoder(
            Foo("foobar", 42),
            JsonToken.BeginObject.left ::
              JsonValue.Key("baz").right ::
              JsonValue.Number(42).right ::
              JsonValue.Key("bar").right ::
              JsonValue.Str("foobar").right ::
              JsonToken.EndObject.left ::
              Nil
          )
        )
      }

      "there are additional fields" in {
        check(
          testDecoder(
            Foo("foobar", 42),
            JsonToken.BeginObject.left ::
              JsonValue.Key("baz").right ::
              JsonValue.Number(42).right ::
              JsonValue.Key("foo").right ::
              JsonValue.Boolean(true).right ::
              JsonValue.Key("foo2").right ::
              JsonValue.Boolean(true).right ::
              JsonValue.Key("bar").right ::
              JsonValue.Str("foobar").right ::
              JsonValue.Key("foo3").right ::
              JsonValue.Boolean(true).right ::
              JsonValue.Key("foo4").right ::
              JsonValue.Boolean(true).right ::
              JsonToken.EndObject.left ::
              Nil
          )
        )
      }
    }

    "decode optional fields" when {

      "it is not provided" in {
        check(
          testDecoder(
            FooOption(None, 42),
            JsonToken.BeginObject.left ::
              JsonValue.Key("baz").right ::
              JsonValue.Number(42).right ::
              JsonToken.EndObject.left ::
              Nil
          )
        )
      }

      "it is provided" in {
        check(
          testDecoder(
            FooOption(Option("It's here!"), 42),
            JsonToken.BeginObject.left ::
              JsonValue.Key("baz").right ::
              JsonValue.Number(42).right ::
              JsonValue.Key("bar").right ::
              JsonValue.Str("It's here!").right ::
              JsonToken.EndObject.left ::
              Nil
          )
        )

      }

      "provided value is null" in {
        check(
          testDecoder(
            FooOption(None, 42),
            JsonToken.BeginObject.left ::
              JsonValue.Key("baz").right ::
              JsonValue.Number(42).right ::
              JsonValue.Key("bar").right ::
              JsonValue.Null.right ::
              JsonToken.EndObject.left ::
              Nil
          )
        )
      }
    }

    // "fail when a required field is missing" in {
    //   check(
    //     testDecoderFailure[FooOption](
    //       "Couldn't find decoded value of baz",
    //       Stream
    //         .emits(
    //           JsonToken.BeginObject ::
    //             JsonToken.Key("bar") ::
    //             JsonToken.Str("baz") ::
    //             JsonToken.EndObject ::
    //             Nil
    //         )
    //         .withPos
    //     )
    //   )
    // }
  }
}
