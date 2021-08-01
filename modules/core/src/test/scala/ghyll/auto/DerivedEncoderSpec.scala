package ghyll.auto

import cats.effect.IO
import ghyll._
import ghyll.json.JsonToken._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class DerivedEncoderSpec extends AnyWordSpec with Matchers with TestEncoder with Checkers {

  case class Foo(bar: String, baz: Map[String, List[Int]])
  val fooDecoder: DerivedEncoder[IO, Foo] = implicitly[DerivedEncoder[IO, Foo]]

  val foo =
    Foo("foobar", Map("baz" -> List(1, 2, 3)))

  val fooRepr =
    List(
      BeginObject,
      Key("bar"),
      Str("foobar"),
      Key("baz"),
      BeginObject,
      Key("baz"),
      BeginArray,
      Number(1),
      Number(2),
      Number(3),
      EndArray,
      EndObject,
      EndObject
    )

  case class FooOption(bar: Option[String], baz: Int)
  val fooOptionDecoder: DerivedEncoder[IO, FooOption] = implicitly[DerivedEncoder[IO, FooOption]]

  "DerivedEncoder.encode" should {
    "encode case classes" when {
      "with values" in {
        check(testEncoder(foo, fooRepr)(fooDecoder))
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
              Number(42),
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
              Number(42),
              EndObject
            )
          )(fooOptionDecoder)
        )
      }
    }
  }
}
