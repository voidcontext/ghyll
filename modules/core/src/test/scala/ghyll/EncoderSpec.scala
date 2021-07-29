package ghyll

import ghyll.Generators._
import org.scalacheck.{Gen, Prop}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import ghyll.json.JsonToken

class EncoderSpec extends AnyWordSpec with Checkers with TestEncoder {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20000)

  "Encoder" should {
    "encode json" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { string =>
            testEncoder(string, JsonToken.Str(string) :: Nil)
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { i =>
            testEncoder(i, JsonToken.Number(i.toString()) :: Nil)
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { b =>
            testEncoder(b, JsonToken.Boolean(b) :: Nil)
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { bigDecimal =>
            testEncoder(bigDecimal, JsonToken.Number(bigDecimal.toString()) :: Nil)
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { localDate =>
            testEncoder(localDate, JsonToken.Str(localDate.toString()) :: Nil)
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testEncoder(ints, JsonToken.BeginArray :: ints.map(i => JsonToken.Number(i.toString())) ++ (JsonToken.EndArray :: Nil))
          }
        )
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testEncoder(maybeInt, maybeInt.fold[JsonToken](JsonToken.Null)(i => JsonToken.Number(i.toString())) :: Nil)
          }
        )
      }

    }
  }
}
