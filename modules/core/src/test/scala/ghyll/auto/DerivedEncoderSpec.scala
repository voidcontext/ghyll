package ghyll.auto

import cats.effect.IO
import ghyll._
import ghyll.json.JsonToken
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class DerivedEncoderSpec extends AnyWordSpec with Matchers with TestEncoder with Checkers {

  case class Foo(bar: String, baz: Map[String, List[Int]])
  val fooDecoder: DerivedEncoder[IO, Foo] = implicitly[DerivedEncoder[IO, Foo]]

  val foo =
    Foo("foobar", Map("baz" -> List(1, 2, 3)))

  val fooRepr: List[JsonToken] =
    List(
      JsonToken.BeginObject,
      JsonToken.Key("bar"),
      JsonToken.Str("foobar"),
      JsonToken.Key("baz"),
      JsonToken.BeginObject,
      JsonToken.Key("baz"),
      JsonToken.BeginArray,
      JsonToken.Number(1),
      JsonToken.Number(2),
      JsonToken.Number(3),
      JsonToken.EndArray,
      JsonToken.EndObject,
      JsonToken.EndObject
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
              JsonToken.BeginObject,
              JsonToken.Key("bar"),
              JsonToken.Str("foobar"),
              JsonToken.Key("baz"),
              JsonToken.Number(42),
              JsonToken.EndObject
            )
          )(fooOptionDecoder)
        )
      }

      "there are not provided optional attributes" in {
        check(
          testEncoder(
            FooOption(None, 42),
            List(
              JsonToken.BeginObject,
              JsonToken.Key("bar"),
              JsonToken.Null,
              JsonToken.Key("baz"),
              JsonToken.Number(42),
              JsonToken.EndObject
            )
          )(fooOptionDecoder)
        )
      }
    }

    "be stack safe" in {
      case class Val(c: Int)
      case class Foo(bar: List[Val])

      val range = List.range(1, 100000)

      testEncoder(
        Foo(range.map(Val(_))),
        JsonToken.BeginObject ::
          JsonToken.Key("bar") ::
          JsonToken.BeginArray ::
          range.flatMap(i =>
            JsonToken.BeginObject :: JsonToken.Key("c") :: JsonToken.Number(i) :: JsonToken.EndObject :: Nil
          ) ++ (JsonToken.EndArray :: JsonToken.EndObject :: Nil)
      )
    }
  }
}
