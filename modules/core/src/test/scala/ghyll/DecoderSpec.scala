package ghyll

// import java.time.LocalDate

// import fs2.Stream
import java.time.LocalDate

import cats.Eq
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.instances.either._
import cats.instances.int._
import cats.instances.list._
import cats.instances.string._
import cats.syntax.eq._
import ghyll.Generators._
import ghyll.json.JsonToken.TokenName
import ghyll.json.{JsonToken, TestJsonTokenReader}
import org.scalacheck.{Gen, Prop}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class DecoderSpec extends AnyWordSpec with Checkers with Matchers with TestDecoder {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 500)

  implicit val eq: Eq[StreamingDecoderError] = Eq.by(_.toString())

  "Decoder" should {
    "decode a JSON" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { case (string) =>
            testDecoder(string, JsonToken.Str(string) :: Nil)
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { case (int) =>
            testDecoder(int, JsonToken.Number(int) :: Nil)
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { case (bool) =>
            testDecoder(bool, JsonToken.Boolean(bool) :: Nil)
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { case (bigDecimal) =>
            testDecoder(bigDecimal, JsonToken.Number(bigDecimal.toString()) :: Nil)
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { case (localDate) =>
            testDecoder(localDate, JsonToken.Str(localDate.toString()) :: Nil)
          }
        )
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testDecoder(
              maybeInt,
              maybeInt.fold[JsonToken](JsonToken.Null)(i => JsonToken.Number(i)) :: Nil
            )
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testDecoder(
              ints,
              List(JsonToken.BeginArray) ++ ints.map(i => JsonToken.Number(i)) ++ List(JsonToken.EndArray)
            )
          }
        )
      }

      "value is a very long List (stack safe)" in {
        check {
          val range = List.range(1, 100000)

          val result = (for {
            reader <- TestJsonTokenReader.withTokens(
                        JsonToken.BeginArray :: range.map(n => JsonToken.Number(n)) ++ List(JsonToken.EndArray)
                      )
            result <- implicitly[Decoder[IO, List[Int]]].decode(reader)
          } yield result).unsafeRunSync()

          (result.eqv(Right(range)): Prop) :| s"${result.map(_.length)} vs ${range.length}"
        }
      }

      "value is a map" in {
        check(
          Prop.forAll(mapOfDates) { case (map) =>
            testDecoder(
              map,
              List(JsonToken.BeginObject) ++
                map
                  .map[List[JsonToken]] { case (k, v) => JsonToken.Key(k) :: JsonToken.Str(v.toString()) :: Nil }
                  .toList
                  .flatten
                ++ List(JsonToken.EndObject)
            )
          }
        )
      }

      "value is a very large map (stack safe)" in {
        check {
          val range = List.range(1, 100000)

          val expected = range.foldLeft(Map.empty[String, Int]) { case (m, i) => m + (i.toString() -> i) }

          val result = (for {
            reader <- TestJsonTokenReader.withTokens(
                        JsonToken.BeginObject :: range.flatMap(n =>
                          JsonToken.Key(n.toString) :: JsonToken.Number(n) :: Nil
                        ) ++ List(JsonToken.EndObject)
                      )
            result <- implicitly[Decoder[IO, Map[String, Int]]].decode(reader)
          } yield result).unsafeRunSync()

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
            s"Expected ${TokenName[JsonToken.Str].show()}, but got ${TokenName[JsonToken.Number[String]].show()} at List()",
            JsonToken.Number("1") :: Nil
          )
        )
      }

      "expected value is a Int, but got something else" in {
        check(
          testDecoderFailure[Int](
            s"Expected ${TokenName[JsonToken.Number[Int]].show()}, but got ${TokenName[JsonToken.BeginObject].show()} at List()",
            JsonToken.BeginObject :: JsonToken.EndObject :: Nil
          )
        )
      }

      "expected value is a Boolean, but got something else" in {
        check(
          testDecoderFailure[Boolean](
            s"Expected ${TokenName[JsonToken.Boolean].show()}, but got ${TokenName[JsonToken.Number[String]].show()} at List()",
            JsonToken.Number("1") :: Nil
          )
        )
      }

      "expected value can be converted to BigDecimal, but got something else" in {
        check(
          testDecoderFailure[BigDecimal](
            s"Expected ${TokenName[JsonToken.Number[BigDecimal]].show()}, but got ${TokenName[JsonToken.Str].show()} at List()",
            JsonToken.Str("something else") :: Nil
          )
        )
      }

      "expected value is a LocalDate, but got something else" in {
        check(
          testDecoderFailure[LocalDate](
            "Text 'tomorrow' could not be parsed at index 0",
            JsonToken.Str("tomorrow") :: Nil
          )
        )
      }
      "expected value is a List, but got something else" in {
        check(
          testDecoderFailure[List[Int]](
            s"Expected ${TokenName[JsonToken.BeginArray].show()}, but got ${TokenName[JsonToken.BeginObject].show()} at List()",
            JsonToken.BeginObject :: JsonToken.EndObject :: Nil
          )
        )
      }
      "expected value is a List, but not all item has the same type" in {
        check(
          testDecoderFailure[List[Int]](
            s"Expected ${TokenName[JsonToken.Number[Int]].show()}, but got ${TokenName[JsonToken.Str].show()} at List(ArrayIndex(2), Array)",
            JsonToken.BeginArray ::
              JsonToken.Number(1) ::
              JsonToken.Number(1) ::
              JsonToken.Str("1") ::
              JsonToken.EndArray :: Nil
          )
        )
      }

      "expected value is a Map, but got something else" in {
        check(
          testDecoderFailure[Map[String, Int]](
            s"Expected ${TokenName[JsonToken.BeginObject].show()}, but got ${TokenName[JsonToken.BeginArray].show()} at List()",
            JsonToken.BeginArray :: JsonToken.EndArray :: Nil
          )
        )
      }

      "expected value is a Optional, but has the wrong type" in {
        check(
          testDecoderFailure[Option[Int]](
            s"Expected ${TokenName[JsonToken.Number[Int]].show()}, but got ${TokenName[JsonToken.Str].show()} at List()",
            JsonToken.Str("1") :: Nil
          )
        )
      }

      // TODO: further error scenarios:
      // - expected end of array|object but never received, etc...
    }
  }
}
