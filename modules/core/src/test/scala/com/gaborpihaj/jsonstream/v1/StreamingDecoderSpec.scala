package com.gaborpihaj.jsonstream.v1

import java.io.File

import cats.effect.IO
import com.gaborpihaj.jsonstream.v1.StreamingDecoder.{decode, Preprocessor}
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StreamingDecoderSpec extends AnyWordSpec with Matchers {
  "decode()" should {
    val file = new File("modules/core/src/test/resources/test-object.json")
    val fileSimple = new File("modules/core/src/test/resources/test-object-simple.json")
    case class Data(value: BigDecimal, additional: Option[List[String]])
    case class Obj(name: String, bool: Option[Boolean], data: Option[Data])

    "decode simple json" in {

      decode[IO, Obj](fileSimple).use {
        _.compile.toList.map {
          _ should be(
            List(
              Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
              Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
              Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
              Right("baz" -> Obj("baz", None, None))
            )
          )
        }
      }
        .unsafeRunSync()
    }

    "decode json" in {

      decode[IO, Obj](file, Preprocessor.dataOnly).use {
        _.compile.toList.map {
          _ should be(
            List(
              Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
              Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
              Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
              Right("baz" -> Obj("baz", None, None))
            )
          )
        }
      }
        .unsafeRunSync()
    }

    "skip keys and decode root" in {
      decode[IO, Obj](file, Preprocessor.root("some-key"), streamValues = false).use {
        _.compile.toList.map {
          _ should be(
            List(
              Right("" -> Obj("foo", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("foobar"))))))
            )
          )
        }
      }
        .unsafeRunSync()

    }
  }
}
