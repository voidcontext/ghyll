package com.gaborpihaj.jsonstream

import java.io.ByteArrayInputStream

import cats.effect.IO
import com.gaborpihaj.jsonstream.StreamingDecoder2.Decoder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StreamingDecoder2Spec extends AnyWordSpec with Matchers {
  case class Foo(bar: String, baz: Int)
  implicit val decoder: Decoder[Foo] = StreamingDecoder2.deriveDecoder

  "decodeObject()" should {

    "decode simple json" in {

      StreamingDecoder2
        .decoder[IO]
        .decodeObject[Foo](new ByteArrayInputStream("""{"bar": "foobar", "baz": 42}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(Right(Foo("foobar", 42)))
          )
        }
        .unsafeRunSync()
    }

    // "decode json" in {

    //   decode[IO, Obj](file, Preprocessor.dataOnly).use {
    //     _.compile.toList.map {
    //       _ should be(
    //         List(
    //           Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
    //           Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
    //           Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
    //           Right("baz" -> Obj("baz", None, None))
    //         )
    //       )
    //     }
    //   }
    //     .unsafeRunSync()
    // }

    // "skip keys and decode root" in {
    //   decode[IO, Obj](file, Preprocessor.root("some-key"), streamValues = false).use {
    //     _.compile.toList.map {
    //       _ should be(
    //         List(
    //           Right("" -> Obj("foo", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("foobar"))))))
    //         )
    //       )
    //     }
    //   }
    //     .unsafeRunSync()

    // }
  }
}
