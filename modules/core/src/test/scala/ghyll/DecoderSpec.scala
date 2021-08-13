package ghyll

// import java.time.LocalDate

// import fs2.Stream
import ghyll.Generators._
// import ghyll.TokenStream._
import ghyll.json.JsonToken
// import ghyll.json.JsonToken.TokenName
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
            testDecoder(string, JsonToken.Str(string) :: Nil)
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { case (int) =>
            testDecoder(int, JsonToken.Number(int.toString()) :: Nil)
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
              maybeInt.fold[JsonToken](JsonToken.Null)(i => JsonToken.Number(i.toString)) :: Nil
            )
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testDecoder(
              ints,
              List(JsonToken.BeginArray) ++ ints.map(i => JsonToken.Number(i.toString())) ++ List(JsonToken.EndArray)
            )
          }
        )
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
    }

    // "return an error result" when {
    //   "expected value is a String, but got something else" in {
    //     check(
    //       testDecoderFailure[String](
    //         s"Expected ${TokenName[JsonToken.Str].show()}, but got ${TokenName[JsonToken.Number[String]].show()} at List()",
    //         Stream.emit(JsonToken.Number("1")).withPos
    //       )
    //     )
    //   }

    //   "expected value is a Int, but got something else" in {
    //     check(
    //       testDecoderFailure[Int](
    //         s"Expected ${TokenName[JsonToken.Number[String]].show()}, but got ${TokenName[JsonToken.BeginObject].show()} at List()",
    //         Stream.emits(JsonToken.BeginObject :: JsonToken.EndObject :: Nil).withPos
    //       )
    //     )
    //   }

    //   "expected value is a Boolean, but got something else" in {
    //     check(
    //       testDecoderFailure[Boolean](
    //         s"Expected ${TokenName[JsonToken.Boolean].show()}, but got ${TokenName[JsonToken.Number[String]].show()} at List()",
    //         Stream.emit(JsonToken.Number("1")).withPos
    //       )
    //     )
    //   }

    //   "expected value can be converted to BigDecimal, but got something else" in {
    //     check(
    //       testDecoderFailure[BigDecimal](
    //         s"Expected ${TokenName[JsonToken.Number[String]].show()}, but got ${TokenName[JsonToken.Str].show()} at List()",
    //         Stream.emit(JsonToken.Str("something else")).withPos
    //       )
    //     )
    //   }

    //   "expected value is a LocalDate, but got something else" in {
    //     check(
    //       testDecoderFailure[LocalDate](
    //         "Text 'tomorrow' could not be parsed at index 0",
    //         Stream.emit(JsonToken.Str("tomorrow")).withPos
    //       )
    //     )
    //   }
    //   "expected value is a List, but got something else" in {
    //     check(
    //       testDecoderFailure[List[Int]](
    //         s"Expected ${TokenName[JsonToken.BeginArray].show()}, but got ${TokenName[JsonToken.BeginObject].show()} at List()",
    //         Stream.emits(JsonToken.BeginObject :: JsonToken.EndObject :: Nil).withPos
    //       )
    //     )
    //   }
    //   "expected value is a List, but not all item has the same type" in {
    //     check(
    //       testDecoderFailure[List[Int]](
    //         s"Expected ${TokenName[JsonToken.Number[String]].show()}, but got ${TokenName[JsonToken.Str].show()} at List(ArrayIndex(2), Array)",
    //         Stream
    //           .emits(
    //             JsonToken.BeginArray :: JsonToken.Number("1") :: JsonToken.Number("2") :: JsonToken.Str(
    //               "3"
    //             ) :: JsonToken.EndArray :: Nil
    //           )
    //           .withPos
    //       )
    //     )
    //   }

    //   "expected value is a Map, but got something else" in {
    //     check(
    //       testDecoderFailure[Map[String, Int]](
    //         s"Expected ${TokenName[JsonToken.BeginObject].show()}, but got ${TokenName[JsonToken.BeginArray].show()} at List()",
    //         Stream.emits(JsonToken.BeginArray :: JsonToken.EndArray :: Nil).withPos
    //       )
    //     )
    //   }

    //   "expected value is a Optional, but has the wrong type" in {
    //     check(
    //       testDecoderFailure[Option[Int]](
    //         s"Expected ${TokenName[JsonToken.Number[String]].show()}, but got ${TokenName[JsonToken.Str].show()} at List()",
    //         Stream.emit(JsonToken.Str("1")).withPos
    //       )
    //     )
    //   }

      // TODO: further error scenarios:
      // - expected end of array|object but never received, etc...
    //}
  }
}
