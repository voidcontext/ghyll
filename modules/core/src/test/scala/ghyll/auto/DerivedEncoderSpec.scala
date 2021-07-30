package ghyll.auto

import ghyll._
import ghyll.json.JsonToken._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.IO

class DerivedEncoderSpec extends AnyWordSpec with Matchers with TestEncoder {

  case class Foo(bar: String, baz: Int)
  val fooDecoder: DerivedEncoder[IO, Foo] = implicitly[DerivedEncoder[IO, Foo]]

  case class FooOption(bar: Option[String], baz: Int)
  val fooOptionDecoder: DerivedEncoder[IO, FooOption] = implicitly[DerivedEncoder[IO, FooOption]]

  case class Root(foo: Foo)

  "DerivedEncoder.encode" should {
    "encode case classes" when {
      "there are simple scalar attributes" in {
        testEncoder(
          Foo("foobar", 42),
          List(
            BeginObject,
            Key("bar"),
            BeginObject,
            Key("baz"),
            Number("42"),
            EndObject,
            EndObject
          )
          )(fooDecoder)
      }

      "there are provided optional attributes" in {
        testEncoder(
          FooOption(Some("foobar"), 42),
          List(
            BeginObject,
            Key("bar"),
            Str("foobar"),
            Key("baz"),
            Number("42"),
            EndObject
          )
          )(fooOptionDecoder)
      }

      "there are not provided optional attributes" in {
        testEncoder(
          FooOption(None, 42),
          List(
            BeginObject,
            Key("baz"),
            Number("42"),
            EndObject
          )
        )(fooOptionDecoder)
      }
    }
  }
}
