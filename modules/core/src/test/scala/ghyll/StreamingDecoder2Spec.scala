package ghyll

import java.io.{ByteArrayInputStream, File, FileInputStream}

import cats.effect.IO
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StreamingDecoder2Spec extends AnyWordSpec with Matchers {
  case class Foo(bar: String, baz: Int)
  implicit val fooDecoder: Decoder[Foo] = StreamingDecoder2.deriveDecoder

  case class FooList(bar: List[Int])
  implicit val fooListDecoder: Decoder[FooList] = StreamingDecoder2.deriveDecoder

  case class FooOption(bar: Option[String], baz: Int)
  implicit val fooOptionDecoder: Decoder[FooOption] = StreamingDecoder2.deriveDecoder

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

    "decode simple json: different field order" in {
      StreamingDecoder2
        .decoder[IO]
        .decodeObject[Foo](new ByteArrayInputStream("""{"baz": 42, "bar": "foobar"}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(Right(Foo("foobar", 42)))
          )
        }
        .unsafeRunSync()
    }

    "decode simple json: skip additional field" in {

      StreamingDecoder2
        .decoder[IO]
        .decodeObject[Foo](new ByteArrayInputStream("""{"baz": 42,"foo": true, "bar": "foobar"}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(Right(Foo("foobar", 42)))
          )
        }
        .unsafeRunSync()
    }

    "decode simple json: decode missing optional fields as None" in {

      StreamingDecoder2
        .decoder[IO]
        .decodeObject[FooOption](new ByteArrayInputStream("""{"baz": 42}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(Right(FooOption(None, 42)))
          )
        }
        .unsafeRunSync()
    }

    "decode simple json: decode provided optional field" in {

      StreamingDecoder2
        .decoder[IO]
        .decodeObject[FooOption](new ByteArrayInputStream("""{"baz": 42, "bar": "It's here!"}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(Right(FooOption(Option("It's here!"), 42)))
          )
        }
        .unsafeRunSync()
    }

    "decode an empty list" in {

      StreamingDecoder2
        .decoder[IO]
        .decodeObject[FooList](new ByteArrayInputStream("""{"bar": []}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(Right(FooList(List())))
          )
        }
        .unsafeRunSync()
    }

    "decode a non empty" in {

      StreamingDecoder2
        .decoder[IO]
        .decodeObject[FooList](new ByteArrayInputStream("""{"bar": [5,4,3, 1, 2]}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(Right(FooList(List(5, 4, 3, 1, 2))))
          )
        }
        .unsafeRunSync()
    }

    "should fail when a required field is missing" in {

      StreamingDecoder2
        .decoder[IO]
        .decodeObject[FooOption](new ByteArrayInputStream("""{"bar": "baz"}""".getBytes()))
        .use { result =>
          IO.delay(
            result should be(a[Left[_, FooOption]])
          )
        }
        .unsafeRunSync()
    }

    "stream keys and values" in {
      val fileSimple = new File("modules/core/src/test/resources/test-object-simple.json")

      case class Data(value: BigDecimal, additional: Option[List[String]])
      implicit val dataDecoder: Decoder[Data] = StreamingDecoder2.deriveDecoder
      case class Obj(name: String, bool: Option[Boolean], data: Option[Data])
      implicit val objDecoder: Decoder[Obj] = StreamingDecoder2.deriveDecoder
      val expected =
        List(
          Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
          Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
          Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
          Right("baz" -> Obj("baz", None, None))
        )

      StreamingDecoder2
        .decoder[IO]
        .decodeKeyValues[Obj](new FileInputStream(fileSimple))
        .use(
          _.compile.toList.map {
            _ should be(expected)
          }
        )
    }

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
