package ghyll.auto

import cats.effect.IO
// import cats.effect.unsafe.implicits.global
// import ghyll.Utils.createReader
//import ghyll._
// import org.scalatest.compatible.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ghyll.TestDecoder
import ghyll.json.JsonToken
import fs2.Stream
import org.scalatestplus.scalacheck.Checkers

class DerivedDecoderSpec extends AnyWordSpec with Matchers with TestDecoder with Checkers {
  case class Foo(bar: String, baz: Int)
  val fooDecoder: DerivedDecoder[IO, Foo] = implicitly[DerivedDecoder[IO, Foo]]

  case class FooOption(bar: Option[String], baz: Int)
  val fooOptionDecoder: DerivedDecoder[IO, FooOption] = implicitly[DerivedDecoder[IO, FooOption]]

  // def testDecoder[A](json: String, expected: StreamingDecoderResult[A], decoder: Decoder[IO, A]): Assertion =
  //   createReader(json)
  //     .use(reader => IO.delay(decoder.decode(reader)))
  //     .map(_ should be(expected))
  //     .unsafeRunSync()

  "DerivedDecoder.decode" should {
    "decode case classes" when {

      "there are only scalar values in the case class" in {
        check(
        testDecoder(Foo("foobar", 42), Stream.emits(
          JsonToken.BeginObject ::
            JsonToken.Key("bar") ::
            JsonToken.Str("foobar") ::
            JsonToken.Key("baz") ::
            JsonToken.Number("42") ::
            JsonToken.EndObject ::
            Nil
        ))
        )
      }

      "field order is different" in {
        check(
        testDecoder(Foo("foobar", 42), Stream.emits(
          JsonToken.BeginObject ::
            JsonToken.Key("baz") ::
            JsonToken.Number("42") ::
            JsonToken.Key("bar") ::
            JsonToken.Str("foobar") ::
            JsonToken.EndObject ::
            Nil
        ))
        )
      }

      "there are additional fields" in {
        check(
        testDecoder(Foo("foobar", 42), Stream.emits(
          JsonToken.BeginObject ::
            JsonToken.Key("baz") ::
            JsonToken.Number("42") ::
            JsonToken.Key("foo") ::
            JsonToken.Boolean(true) ::
            JsonToken.Key("bar") ::
            JsonToken.Str("foobar") ::
            JsonToken.EndObject ::
            Nil
        )))
      }
    }

    "decode optional fields" when {

      "it is not provided" in {
        check(
        testDecoder(FooOption(None, 42), Stream.emits(
          JsonToken.BeginObject ::
            JsonToken.Key("baz") ::
            JsonToken.Number("42") ::
            JsonToken.EndObject ::
            Nil
        )))
      }

      "it is provided" in {
        check(
        testDecoder(FooOption(Option("It's here!"), 42), Stream.emits(
          JsonToken.BeginObject ::
            JsonToken.Key("baz") ::
            JsonToken.Number("42") ::
            JsonToken.Key("bar") ::
            JsonToken.Str("It's here!") ::
            JsonToken.EndObject ::
            Nil
        )))

      }

      "provided value is null" in {
        check(
        testDecoder(FooOption(None, 42), Stream.emits(
          JsonToken.BeginObject ::
            JsonToken.Key("baz") ::
            JsonToken.Number("42") ::
            JsonToken.Key("bar") ::
            JsonToken.Null ::
            JsonToken.EndObject ::
            Nil
        )))
//        testDecoder("""{"baz": 42, "bar": null}""", Right(FooOption(None, 42)), fooOptionDecoder)
      }
    }

    "fail when a required field is missing" in {
      check(
      testDecoderFailure[FooOption](
        "Couldn't find decoded value of baz",
        Stream.emits(
                  JsonToken.BeginObject ::
            JsonToken.Key("bar") ::
            JsonToken.Str("baz") ::
            JsonToken.EndObject ::
            Nil)
      )
      )
    }
  }
}
