package ghyll.auto

import cats.effect.IO
import ghyll.Utils.createReader
import ghyll._
import org.scalatest.compatible.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DerivedDecoderSpec extends AnyWordSpec with Matchers {
  case class Foo(bar: String, baz: Int)
  val fooDecoder: DerivedDecoder[Foo] = implicitly[DerivedDecoder[Foo]]

  case class FooOption(bar: Option[String], baz: Int)
  val fooOptionDecoder: DerivedDecoder[FooOption] = implicitly[DerivedDecoder[FooOption]]

  def testDecoder[A](json: String, expected: StreamingDecoderResult[A], decoder: Decoder[A]): Assertion =
    createReader(json)
      .use(reader => IO.delay(decoder.decode(reader)))
      .map(_ should be(expected))
      .unsafeRunSync()

  "DerivedDecoder.decode" should {
    "decode case classes" when {

      "there are only scalar values in the case class" in {
        testDecoder("""{"bar": "foobar", "baz": 42}""", Right(Foo("foobar", 42)), fooDecoder)
      }

      "field order is different" in {
        testDecoder("""{"baz": 42, "bar": "foobar"}""", Right(Foo("foobar", 42)), fooDecoder)
      }

      "there are additional fields" in {
        testDecoder("""{"baz": 42,"foo": true, "bar": "foobar"}""", Right(Foo("foobar", 42)), fooDecoder)
      }
    }

    "decode optional fields" when {

      "it is not provided" in {

        testDecoder("""{"baz": 42}""", Right(FooOption(None, 42)), fooOptionDecoder)
      }

      "it is provided" in {

        testDecoder(
          """{"baz": 42, "bar": "It's here!"}""",
          Right(FooOption(Option("It's here!"), 42)),
          fooOptionDecoder
        )
      }

      "provided value is null" in {
        testDecoder("""{"baz": 42, "bar": null}""", Right(FooOption(None, 42)), fooOptionDecoder)
      }
    }

    "fail when a required field is missing" in {
      testDecoder(
        """{"bar": "baz"}""",
        Left(StreamingDecodingFailure("Couldn't find decoded value of baz")),
        fooOptionDecoder
      )
    }
  }
}
