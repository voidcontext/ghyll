package ghyll.auto

// import cats.effect.IO
import ghyll._
import ghyll.json.JsonToken
import ghyll.json.JsonValue
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import cats.effect.IO
import ghyll.json.JsonTokenReader.JsonTokenReaderResult
import ghyll.utils.EitherOps
import ghyll.json.JsonTokenWriter
import cats.effect.unsafe.implicits.global

class DerivedEncoderSpec extends AnyWordSpec with Matchers with TestEncoder with Checkers {

  case class Foo(bar: String, baz: Map[String, List[Int]])
  val fooDecoder: DerivedEncoder[IO, Foo] = implicitly[DerivedEncoder[IO, Foo]]

  val foo =
    Foo("foobar", Map("baz" -> List(1, 2, 3)))

  val fooRepr: List[JsonTokenReaderResult] =
    List(
      JsonToken.BeginObject.left,
      JsonValue.Key("bar").right,
      JsonValue.Str("foobar").right,
      JsonValue.Key("baz").right,
      JsonToken.BeginObject.left,
      JsonValue.Key("baz").right,
      JsonToken.BeginArray.left,
      JsonValue.Number(1).right,
      JsonValue.Number(2).right,
      JsonValue.Number(3).right,
      JsonToken.EndArray.left,
      JsonToken.EndObject.left,
      JsonToken.EndObject.left
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
              JsonToken.BeginObject.left,
              JsonValue.Key("bar").right,
              JsonValue.Str("foobar").right,
              JsonValue.Key("baz").right,
              JsonValue.Number(42).right,
              JsonToken.EndObject.left
            )
          )(fooOptionDecoder)
        )
      }

      "there are not provided optional attributes" in {
        check(
          testEncoder(
            FooOption(None, 42),
            List(
              JsonToken.BeginObject.left,
              JsonValue.Key("bar").right,
              JsonValue.Null.right,
              JsonValue.Key("baz").right,
              JsonValue.Number(42).right,
              JsonToken.EndObject.left
            )
          )(fooOptionDecoder)
        )
      }
    }

    "be stack safe" in {
      case class Val(c: Int)
      case class Foo(bar: List[Val])

      def writer: JsonTokenWriter[IO] = ???

      implicitly[DerivedEncoder[IO, Foo]].encode(Foo(List.range(1, 100000).map(Val(_))), writer).unsafeRunSync() shouldBe a[Right[_, _]]
    }
  }
}
