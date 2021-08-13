package ghyll

import java.io.ByteArrayInputStream

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ghyll.Pos._
import ghyll.json.JsonToken
import ghyll.json.JsonToken._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TokenStreamSpec extends AnyWordSpec with Matchers with TestTokenStream {
  "TokenStream.fromJson" should {
    "convert a simple object to TokenStream" in {
      val json = """{"foo": "did it work?", "bar": 42, "baz": null}"""
      TokenStream
        .fromJson[IO](new ByteArrayInputStream(json.getBytes()))
        .use(s => IO.delay(s.toList))
        .unsafeRunSync() should be(
        List[Either[TokeniserError, (JsonToken, List[Pos])]](
          Right(BeginObject -> List()),
          Right(Key("foo") -> List(Obj)),
          Right(Str("did it work?") -> List(ObjectKey("foo"), Obj)),
          Right(Key("bar") -> List(Obj)),
          Right(Number("42") -> List(ObjectKey("bar"), Obj)),
          Right(Key("baz") -> List(Obj)),
          Right(Null -> List(ObjectKey("baz"), Obj)),
          Right(EndObject -> List(Obj))
        )
      )
    }
  }

  "TokenStream.withPos" should {
    "preserve the orginal tokens" in {
      val tokens = wrapTokens(List(
        BeginObject,
        Key("foo"),
        Str("bar"),
        Key("baz"),
        BeginObject,
        Key("something"),
        Null,
        EndObject,
        EndObject
      ))

        TokenStream
          .withPos(tokens)
          .toList.map(_.map(_._1)) should be(tokens)
    }
  }

  "TokenStream.skipValue" should {
    "skip scalar value" in {
      val tokens = tokenStream(Str("foo") :: Number("1") :: Str("bar") :: Nil)
        TokenStream
          .skipValue(tokens)
          .toList should be(tokens.tail)

    }

    "skip a full object" in {
      val obj1 = tokenStream(List(BeginObject, Key("foo"), Number("1"), Key("bar"), Number("2"), EndObject))
      val rest = tokenStream(List(Key("obj2"), BeginObject, EndObject))

        TokenStream
          .skipValue(obj1 ++ rest)
          .toList should be(rest)
    }

    "skip a full list" in {
      val array1 = tokenStream(List(BeginArray, Number("1"), Number("2"), EndArray))
      val rest = tokenStream(List(Key("obj2"), BeginArray, EndArray))

        TokenStream
          .skipValue(array1 ++ rest)
          .toList should be(rest)
    }
  }
}
