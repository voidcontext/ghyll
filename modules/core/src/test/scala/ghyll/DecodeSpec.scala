package ghyll

import java.io.{ByteArrayInputStream, File, FileInputStream}

import cats.effect.IO
import ghyll.jsonpath._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DecodeSpec extends AnyWordSpec with Matchers {
  case class Foo(bar: String, baz: Int)
  implicit val fooDecoder: Decoder[Foo] = deriveDecoder

  case class FooList(bar: List[Int])
  implicit val fooListDecoder: Decoder[FooList] = deriveDecoder

  case class FooOption(bar: Option[String], baz: Int)
  implicit val fooOptionDecoder: Decoder[FooOption] = deriveDecoder

  case class Data(value: BigDecimal, additional: Option[List[String]])
  implicit val dataDecoder: Decoder[Data] = deriveDecoder
  case class Obj(name: String, bool: Option[Boolean], data: Option[Data])
  implicit val objDecoder: Decoder[Obj] = deriveDecoder

  val fileSimple = new File("modules/core/src/test/resources/test-object-simple.json")
  val fileNested = new File("modules/core/src/test/resources/test-object.json")

  "decodeObject()" should {

    "decode simple JSON (not nested)" when {

      "there are only scalar values in the JSON" in {

        decodeObject[IO, Foo](new ByteArrayInputStream("""{"bar": "foobar", "baz": 42}""".getBytes())).use { result =>
          IO.delay(
            result should be(Right(Foo("foobar", 42)))
          )
        }
          .unsafeRunSync()
      }

      "field order is different" in {
        decodeObject[IO, Foo](new ByteArrayInputStream("""{"baz": 42, "bar": "foobar"}""".getBytes())).use { result =>
          IO.delay(
            result should be(Right(Foo("foobar", 42)))
          )
        }
          .unsafeRunSync()
      }

      "there are additional fields" in {

        decodeObject[IO, Foo](new ByteArrayInputStream("""{"baz": 42,"foo": true, "bar": "foobar"}""".getBytes())).use {
          result =>
            IO.delay(
              result should be(Right(Foo("foobar", 42)))
            )
        }
          .unsafeRunSync()
      }
    }

    "decode optional fields" when {

      "it is not provided" in {

        decodeObject[IO, FooOption](new ByteArrayInputStream("""{"baz": 42}""".getBytes())).use { result =>
          IO.delay(
            result should be(Right(FooOption(None, 42)))
          )
        }
          .unsafeRunSync()
      }

      "it is provided" in {

        decodeObject[IO, FooOption](new ByteArrayInputStream("""{"baz": 42, "bar": "It's here!"}""".getBytes())).use {
          result =>
            IO.delay(
              result should be(Right(FooOption(Option("It's here!"), 42)))
            )
        }
          .unsafeRunSync()
      }

      "provided value is null" in {
        decodeObject[IO, FooOption](new ByteArrayInputStream("""{"baz": 42, "bar": null}""".getBytes())).use { result =>
          IO.delay(
            result should be(Right(FooOption(None, 42)))
          )
        }
          .unsafeRunSync()
      }
    }

    "decode a list" when {

      "it's empty list" in {

        decodeObject[IO, FooList](new ByteArrayInputStream("""{"bar": []}""".getBytes())).use { result =>
          IO.delay(
            result should be(Right(FooList(List())))
          )
        }
          .unsafeRunSync()
      }

      "it's not empty" in {

        decodeObject[IO, FooList](new ByteArrayInputStream("""{"bar": [5,4,3, 1, 2]}""".getBytes())).use { result =>
          IO.delay(
            result should be(Right(FooList(List(5, 4, 3, 1, 2))))
          )
        }
          .unsafeRunSync()
      }
    }

    "fail when a required field is missing" in {

      decodeObject[IO, FooOption](new ByteArrayInputStream("""{"bar": "baz"}""".getBytes())).use { result =>
        IO.delay(
          result should be(a[Left[_, FooOption]])
        )
      }
        .unsafeRunSync()
    }

    "only decode the object under the given path" in {
      decodeObject[IO, Obj]("data" >:: "bar-2" >:: JNil, new FileInputStream(fileNested)).use { result =>
        IO.delay(
          result should be(Right(Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))))
        )
      }
    }
  }

  "decodeKeyValues" should {

    "stream keys and values" in {
      val expected =
        List(
          Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
          Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
          Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
          Right("baz" -> Obj("baz", None, None))
        )

      decodeKeyValues[IO, Obj](new FileInputStream(fileSimple))
        .use(
          _.compile.toList.map {
            _ should be(expected)
          }
        )
        .unsafeRunSync()
    }

    "only decode the object under the given path" in {

      val expected =
        List(
          Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
          Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
          Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
          Right("baz" -> Obj("baz", None, None))
        )

      decodeKeyValues[IO, Obj]("data" >:: JNil, new FileInputStream(fileNested))
        .use(
          _.compile.toList.map {
            _ should be(expected)
          }
        )
        .unsafeRunSync()
    }
  }
}
