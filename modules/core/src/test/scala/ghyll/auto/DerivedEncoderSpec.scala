package ghyll.auto

import cats.effect.IO
import ghyll._
import ghyll.json.JsonToken._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class DerivedEncoderSpec extends AnyWordSpec with Matchers with TestEncoder with Checkers {

  case class Foo(bar: String, baz: Int)
  val fooDecoder: DerivedEncoder[IO, Foo] = implicitly[DerivedEncoder[IO, Foo]]

  case class FooOption(bar: Option[String], baz: Int)
  val fooOptionDecoder: DerivedEncoder[IO, FooOption] = implicitly[DerivedEncoder[IO, FooOption]]

  case class Root(foo: Foo)

  "DerivedEncoder.encode" should {
    "encode case classes" when {
      "there are simple scalar attributes" in {
        check(
          testEncoder(
            Foo("foobar", 42),
            List(
              BeginObject,
              Key("bar"),
              Str("foobar"),
              Key("baz"),
              Number("42"),
              EndObject
            )
          )(fooDecoder)
        )
      }

      "there are provided optional attributes" in {
        check(
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
        )
      }

      "there are not provided optional attributes" in {
        check(
          testEncoder(
            FooOption(None, 42),
            List(
              BeginObject,
              Key("bar"),
              Null,
              Key("baz"),
              Number("42"),
              EndObject
            )
          )(fooOptionDecoder)
        )
      }
    }
  }
}
