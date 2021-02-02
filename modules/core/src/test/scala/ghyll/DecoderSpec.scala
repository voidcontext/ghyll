package ghyll

import java.time.{LocalDate, ZoneId}

import cats.effect.IO
import ghyll.Utils.createReader
import io.circe.Encoder
import io.circe.syntax._
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.claimant.Claim

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
class DecoderSpec extends AnyWordSpec with Checkers with Matchers {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 500)

  val string: Gen[String] = Gen.oneOf(Gen.asciiStr, Arbitrary.arbString.arbitrary)
  val int: Gen[Int] = Arbitrary.arbInt.arbitrary
  val boolean: Gen[Boolean] = Gen.oneOf(false, true)
  val bigDecimal: Gen[BigDecimal] = Arbitrary.arbBigDecimal.arbitrary
  val localDate: Gen[LocalDate] =
    Arbitrary.arbDate.arbitrary.map(_.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())

  def testDecoder[A: Encoder](value: A)(implicit decoder: Decoder[A]) =
    createReader(value.asJson.toString).use { reader =>
      IO.delay(decoder.decode(reader))
    }
      .map(decoded => Claim(decoded == Right(value)))
      .unsafeRunSync()

  "Decoder" should {
    "decode a JSON" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { case (string) =>
            testDecoder(Map("foo" -> string))
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { case (int) =>
            testDecoder(Map("foo" -> int))
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { case (bool) =>
            testDecoder(Map("foo" -> bool))
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { case (bigDecimal) =>
            testDecoder(Map("foo" -> bigDecimal))
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { case (localDate) =>
            testDecoder(Map("foo" -> localDate))
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testDecoder(Map("foo" -> ints))
          }
        )
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testDecoder(Map("foo" -> maybeInt))
          }
        )
      }
    }

    "return an error result" when {
      "expected value is a String, but got something else" in {
        val value = Map("foo" -> 1)
        createReader(value.asJson.toString).use { reader =>
          IO.delay(Decoder[Map[String, String]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected STRING, but got NUMBER"))))
          .unsafeRunSync()
      }

      "expected value is a Int, but got something else" in {
        val value = Map("foo" -> Map.empty[String, String])
        createReader(value.asJson.toString).use { reader =>
          IO.delay(Decoder[Map[String, Int]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got BEGIN_OBJECT"))))
          .unsafeRunSync()
      }
      "expected value is a Boolean, but got something else" in {
        val value = Map("foo" -> 1)
        createReader(value.asJson.toString).use { reader =>
          IO.delay(Decoder[Map[String, Boolean]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected BOOLEAN, but got NUMBER"))))
          .unsafeRunSync()
      }
      "expected value can be converted to BigDecimal, but got something else" in {
        val value = Map("foo" -> "something else")
        createReader(value.asJson.toString).use { reader =>
          IO.delay(Decoder[Map[String, BigDecimal]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got STRING"))))
          .unsafeRunSync()
      }
      "expected value is a LocalDate, but got something else" in {
        val value = Map("foo" -> "tomorrow")
        createReader(value.asJson.toString).use { reader =>
          IO.delay(Decoder[Map[String, LocalDate]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Text 'tomorrow' could not be parsed at index 0"))))
          .unsafeRunSync()
      }
      "expected value is a List, but got something else" in {
        val value = Map("foo" -> Map.empty[String, String])
        createReader(value.asJson.toString).use { reader =>
          IO.delay(Decoder[Map[String, Int]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got BEGIN_OBJECT"))))
          .unsafeRunSync()
      }
      "expected value is a List, but not all item has the same type" in {
        createReader("""{"foo": [1, 2, "str"]}""").use { reader =>
          IO.delay(Decoder[Map[String, List[Int]]].decode(reader))
        }
          .map(_ should be(Left(StreamingDecodingFailure("Expected NUMBER, but got STRING"))))
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
