package ghyll

import java.time.LocalDate

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ghyll.Generators._
import ghyll.Utils.{createReader, escape}
import org.scalacheck.{Gen, Prop}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class DecoderSpec extends AnyWordSpec with Checkers with Matchers with TestDecoder {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 500)

  "Decoder" should {
    "decode a JSON" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { case (string) =>
            testDecoder(Map("foo" -> string), s"""{"foo":"${escape(string)}"}""")
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { case (int) =>
            testDecoder(Map("foo" -> int), s"""{"foo":$int}""")
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { case (bool) =>
            testDecoder(Map("foo" -> bool), s"""{"foo":${if (bool) "true" else "false"}}""")
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { case (bigDecimal) =>
            testDecoder(Map("foo" -> bigDecimal), s"""{"foo":${bigDecimal.toString()}}""")
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { case (localDate) =>
            testDecoder(Map("foo" -> localDate), s"""{"foo":"${localDate.toString()}"}""")
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testDecoder(Map("foo" -> ints), s"""{"foo":[${ints.map(_.toString()).mkString(",")}]}""")
          }
        )
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testDecoder(Map("foo" -> maybeInt), s"""{"foo":${maybeInt.fold("null")(_.toString())}}""")
          }
        )
      }
    }

    "return an error result" when {
      "expected value is a String, but got something else" in {
        createReader("""{"foo":1}""").use { reader =>
          IO.delay(Decoder[Map[String, String]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected STRING, but got NUMBER"))))
          .unsafeRunSync()
      }

      "expected value is a Int, but got something else" in {
        createReader("""{"foo": {}}""").use { reader =>
          IO.delay(Decoder[Map[String, Int]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got BEGIN_OBJECT"))))
          .unsafeRunSync()
      }
      "expected value is a Boolean, but got something else" in {
        createReader("""{"foo": 1}"""").use { reader =>
          IO.delay(Decoder[Map[String, Boolean]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected BOOLEAN, but got NUMBER"))))
          .unsafeRunSync()
      }
      "expected value can be converted to BigDecimal, but got something else" in {
        createReader("""{"foo": "something else"}""").use { reader =>
          IO.delay(Decoder[Map[String, BigDecimal]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got STRING"))))
          .unsafeRunSync()
      }
      "expected value is a LocalDate, but got something else" in {
        createReader("""{"foo": "tomorrow"}""").use { reader =>
          IO.delay(Decoder[Map[String, LocalDate]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Text 'tomorrow' could not be parsed at index 0"))))
          .unsafeRunSync()
      }
      "expected value is a List, but got something else" in {
        createReader("""{"foo": {}}""").use { reader =>
          IO.delay(Decoder[Map[String, List[Int]]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected BEGIN_ARRAY, but got BEGIN_OBJECT"))))
          .unsafeRunSync()
      }
      "expected value is a List, but not all item has the same type" in {
        createReader("""{"foo": [1, 2, "str"]}""").use { reader =>
          IO.delay(Decoder[Map[String, List[Int]]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got STRING"))))
          .unsafeRunSync()
      }

      "expected value is a Map, but got something else" in {
        createReader("""{"foo": []}""").use { reader =>
          IO.delay(Decoder[Map[String, Map[String, Int]]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected BEGIN_OBJECT, but got BEGIN_ARRAY"))))
          .unsafeRunSync()
      }

      "expected value is a Optional, but has the wrong type" in {
        createReader("""{"foo": "1"}""").use { reader =>
          IO.delay(Decoder[Map[String, Option[Int]]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got STRING"))))
          .unsafeRunSync()
      }
    }
  }
}
