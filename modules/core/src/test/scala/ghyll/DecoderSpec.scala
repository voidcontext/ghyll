package ghyll

// import java.time.LocalDate

// import fs2.Stream
import java.time.LocalDate

import cats.kernel.Eq
import ghyll.Generators._
import ghyll.json.JsonToken
import ghyll.json.JsonToken.{TokenName, Delimiter}
import org.scalacheck.{Gen, Prop}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import ghyll.json.JsonValue
import ghyll.utils.EitherOps
import ghyll.json.JsonTokenReader.JsonTokenReaderResult
import ghyll.json.TestJsonTokenReader
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.instances.either._
import cats.instances.int._
import cats.syntax.eq._

class DecoderSpec extends AnyWordSpec with Checkers with Matchers with TestDecoder {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 500)

  implicit val eq: Eq[StreamingDecoderError] = Eq.by(_.toString())

  "Decoder" should {
    "decode a JSON" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { case (string) =>
            testDecoder(string, JsonValue.Str(string).right :: Nil)
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { case (int) =>
            testDecoder(int, JsonValue.Number(int).right :: Nil)
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { case (bool) =>
            testDecoder(bool, JsonValue.Boolean(bool).right :: Nil)
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { case (bigDecimal) =>
            testDecoder(bigDecimal, JsonValue.Number(bigDecimal.toString()).right :: Nil)
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { case (localDate) =>
            testDecoder(localDate, JsonValue.Str(localDate.toString()).right :: Nil)
          }
        )
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testDecoder(
              maybeInt,
              maybeInt.fold[JsonTokenReaderResult](JsonValue.Null.right[Delimiter])(i => JsonValue.Number(i).right) :: Nil
            )
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testDecoder(
              ints,
              List(JsonToken.BeginArray.left) ++ ints.map(i => JsonValue.Number(i).right) ++ List(JsonToken.EndArray.left)
            )
          }
        )
      }

      "value is a very long List (stack safe)" in {
        check {
          val range = List.range(1, 100000)

          val result = implicitly[Decoder[IO, List[Int]]].decode(
            TestJsonTokenReader.withTokens(
              JsonToken.BeginArray.left :: range.map(n => JsonValue.Number(n).right) ++ List(JsonToken.EndArray.left)
            )
          ).unsafeRunSync()

          ((result.eqv(Right(range))): Prop) :| s"${result.map(_.length)} vs ${range.length}"
        }
      }

      "value is a map" in {
        check(
          Prop.forAll(mapOfDates) { case (map) =>
            testDecoder(
              map,
              List(JsonToken.BeginObject.left) ++
                map
                  .map[List[JsonTokenReaderResult]] { case (k, v) => JsonValue.Key(k).right :: JsonValue.Str(v.toString()).right :: Nil }
                  .toList
                  .flatten
                ++ List(JsonToken.EndObject.left)
            )
          }
        )
      }

      "value is a very large map (stack safe)" in {
        check {
          val range = List.range(1, 100000)

          val expected = range.foldLeft(Map.empty[String, Int]) { case (m, i) => m + (i.toString() -> i) }

          val result = implicitly[Decoder[IO, Map[String, Int]]].decode(
            TestJsonTokenReader.withTokens(
              JsonToken.BeginObject.left :: range.flatMap(n =>
                JsonValue.Key(n.toString).right :: JsonValue.Number(n).right :: Nil
              ) ++ List(JsonToken.EndObject.left)
            )
          ).unsafeRunSync()

          ((result
            .map(_.toSet.diff(expected.toSet))
            .eqv(Right(Set.empty))): Prop) :| s"${result.map(_.toList.length)} vs ${range.length}"
        }
      }
    }

    "return an error result" when {
      "expected value is a String, but got something else" in {
        check(
          testDecoderFailure[String](
            s"Expected ${TokenName[JsonToken.Str].show()}, but got ${TokenName[JsonToken.Number].show()} at List()",
            JsonValue.Number("1").right :: Nil
          )
        )
      }

      "expected value is a Int, but got something else" in {
        check(
          testDecoderFailure[Int](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.BeginObject].show()} at List()",
            JsonToken.BeginObject.left :: JsonToken.EndObject.left :: Nil
          )
        )
      }

      "expected value is a Boolean, but got something else" in {
        check(
          testDecoderFailure[Boolean](
            s"Expected ${TokenName[JsonToken.Boolean].show()}, but got ${TokenName[JsonToken.Number].show()} at List()",
            JsonValue.Number("1").right :: Nil
          )
        )
      }

      "expected value can be converted to BigDecimal, but got something else" in {
        check(
          testDecoderFailure[BigDecimal](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.Str].show()} at List()",
            JsonValue.Str("something else").right :: Nil
          )
        )
      }

      "expected value is a LocalDate, but got something else" in {
        check(
          testDecoderFailure[LocalDate](
            "Text 'tomorrow' could not be parsed at index 0",
            JsonValue.Str("tomorrow").right :: Nil
          )
        )
      }
      "expected value is a List, but got something else" in {
        check(
          testDecoderFailure[List[Int]](
            s"Expected ${TokenName[JsonToken.BeginArray].show()}, but got ${TokenName[JsonToken.BeginObject].show()} at List()",
            JsonToken.BeginObject.left :: JsonToken.EndObject.left :: Nil
          )
        )
      }
      "expected value is a List, but not all item has the same type" in {
        check(
          testDecoderFailure[List[Int]](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.Str].show()} at List(ArrayIndex(2), Array)",
            JsonToken.BeginArray.left ::
              JsonValue.Number(1).right ::
              JsonValue.Number(1).right ::
              JsonValue.Str("1").right ::
              JsonToken.EndArray.left :: Nil
          )
        )
      }

      "expected value is a Map, but got something else" in {
        check(
          testDecoderFailure[Map[String, Int]](
            s"Expected ${TokenName[JsonToken.BeginObject].show()}, but got ${TokenName[JsonToken.BeginArray].show()} at List()",
            JsonToken.BeginArray.left :: JsonToken.EndArray.left :: Nil
          )
        )
      }

      "expected value is a Optional, but has the wrong type" in {
        check(
          testDecoderFailure[Option[Int]](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.Str].show()} at List()",
            JsonValue.Str("1").right :: Nil
          )
        )
      }

      // TODO: further error scenarios:
      // - expected end of array|object but never received, etc...
    }
  }
}
