package ghyll

// import java.time.LocalDate

// import cats.effect.IO
// import cats.effect.unsafe.implicits.global
import java.time.LocalDate

import fs2.Stream
import ghyll.Generators._
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName
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
            testDecoder(string, Stream.emit(JsonToken.Str(string)))
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { case (int) =>
            testDecoder(int, Stream.emit(JsonToken.Number(int.toString())))
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { case (bool) =>
            testDecoder(bool, Stream.emit(JsonToken.Boolean(bool)))
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { case (bigDecimal) =>
            testDecoder(bigDecimal, Stream.emit(JsonToken.Number(bigDecimal.toString())))
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { case (localDate) =>
            testDecoder(localDate, Stream.emit(JsonToken.Str(localDate.toString())))
          }
        )
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testDecoder(
              maybeInt,
              Stream.emit(maybeInt.fold[JsonToken](JsonToken.Null)(i => JsonToken.Number(i.toString)))
            )
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testDecoder(
              ints,
              Stream.emit(JsonToken.BeginArray) ++ Stream.emits(ints.map(i => JsonToken.Number(i.toString()))) ++ Stream
                .emit(JsonToken.EndArray)
            )
          }
        )
      }

      "value is a map" in {
        check(
          Prop.forAll(mapOfDates) { case (map) =>
            testDecoder(
              map,
              Stream.emit(JsonToken.BeginObject) ++ Stream.emits(
                map
                  .map[List[JsonToken]] { case (k, v) => JsonToken.Key(k) :: JsonToken.Str(v.toString()) :: Nil }
                  .toList
                  .flatten
              ) ++ Stream.emit(JsonToken.EndObject)
            )
          }
        )
      }

    }

    "return an error result" when {
      "expected value is a String, but got something else" in {
        check(
          testDecoderFailure[String](
            s"Expected ${TokenName[JsonToken.Str].show()}, but got ${TokenName[JsonToken.Number].show()}",
            Stream.emit(JsonToken.Number("1"))
          )
        )
      }

      "expected value is a Int, but got something else" in {
        check(
          testDecoderFailure[Int](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.BeginObject].show()}",
            Stream.emits(JsonToken.BeginObject :: JsonToken.EndObject :: Nil)
          )
        )
      }

      "expected value is a Boolean, but got something else" in {
        check(
          testDecoderFailure[Boolean](
            s"Expected ${TokenName[JsonToken.Boolean].show()}, but got ${TokenName[JsonToken.Number].show()}",
            Stream.emit(JsonToken.Number("1"))
          )
        )
      }

      "expected value can be converted to BigDecimal, but got something else" in {
        check(
          testDecoderFailure[BigDecimal](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.Str].show()}",
            Stream.emit(JsonToken.Str("something else"))
          )
        )
      }

      "expected value is a LocalDate, but got something else" in {
        check(
          testDecoderFailure[LocalDate](
            "Text 'tomorrow' could not be parsed at index 0",
            Stream.emit(JsonToken.Str("tomorrow"))
          )
        )
      }
      "expected value is a List, but got something else" in {
        check(
          testDecoderFailure[List[Int]](
            s"Expected ${TokenName[JsonToken.BeginArray].show()}, but got ${TokenName[JsonToken.BeginObject].show()}",
            Stream.emits(JsonToken.BeginObject :: JsonToken.EndObject :: Nil)
          )
        )
      }
      "expected value is a List, but not all item has the same type" in {
        check(
          testDecoderFailure[List[Int]](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.Str].show()}",
            Stream.emits(
              JsonToken.BeginArray :: JsonToken.Number("1") :: JsonToken.Number("2") :: JsonToken.Str(
                "3"
              ) :: JsonToken.EndArray :: Nil
            )
          )
        )
      }

      "expected value is a Map, but got something else" in {
        check(
          testDecoderFailure[Map[String, Int]](
            s"Expected ${TokenName[JsonToken.BeginObject].show()}, but got ${TokenName[JsonToken.BeginArray].show()}",
            Stream.emits(JsonToken.BeginArray :: JsonToken.EndArray :: Nil)
          )
        )
      }

      "expected value is a Optional, but has the wrong type" in {
        check(
          testDecoderFailure[Option[Int]](
            s"Expected ${TokenName[JsonToken.Number].show()}, but got ${TokenName[JsonToken.Str].show()}",
            Stream.emit(JsonToken.Str("1"))
          )
        )
      }

      // TODO: further error scenarios:
      // - expected end of array|object but never received, etc...
    }
  }
}
