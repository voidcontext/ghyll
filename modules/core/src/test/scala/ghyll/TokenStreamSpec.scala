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
        .use(_.compile.toList)
        .unsafeRunSync() should be(
        List[(JsonToken, List[Pos])](
          BeginObject -> List(),
          Key("foo") -> List(Obj),
          Str("did it work?") -> List(ObjectKey("foo"), Obj),
          Key("bar") -> List(Obj),
          Number("42") -> List(ObjectKey("bar"), Obj),
          Key("baz") -> List(Obj),
          Null -> List(ObjectKey("baz"), Obj),
          EndObject -> List(Obj)
        )
      )
    }
  }

  "TokenStream.withPos" should {
    "preserve the orginal tokens" in {
      val tokens: List[JsonToken] = List(
        BeginObject,
        Key("foo"),
        Str("bar"),
        Key("baz"),
        BeginObject,
        Key("something"),
        Null,
        EndObject,
        EndObject
      )

      withTokens(tokens) { stream =>
        TokenStream
          .withPos(stream)
          .compile
          .toList
          .map(
            _.map(_._1) should be(tokens)
          )
      }
    }
  }

  "TokenStream.skipValue" should {
    "skip scalar value" in {
      val tokens = Str("foo") :: Number("1") :: Str("bar") :: Nil
      withTokenStream(tokens) { stream =>
        TokenStream
          .skipValue(stream)
          .compile
          .toList
          .map(
            _.map(_._1) should be(tokens.tail)
          )
      }
    }

    "skip a full object" in {
      val obj1 = List(BeginObject, Key("foo"), Number("1"), Key("bar"), Number("2"), EndObject)
      val rest = List(Key("obj2"), BeginObject, EndObject)
      withTokenStream(obj1 ++ rest) { stream =>
        TokenStream
          .skipValue(stream)
          .compile
          .toList
          .map(
            _.map(_._1) should be(rest)
          )
      }
    }

    "skip a full list" in {
      val array1 = List(BeginArray, Number("1"), Number("2"), EndArray)
      val rest = List(Key("obj2"), BeginArray, EndArray)
      withTokenStream(array1 ++ rest) { stream =>
        TokenStream
          .skipValue(stream)
          .compile
          .toList
          .map(
            _.map(_._1) should be(rest)
          )
      }
    }
  }
}
