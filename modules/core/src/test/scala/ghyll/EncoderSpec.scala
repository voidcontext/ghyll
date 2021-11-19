package ghyll

import cats.instances.either._
import cats.instances.lazyList._
import cats.kernel.Eq
import cats.syntax.eq._
import ghyll.Generators._
import ghyll.json.JsonToken
import org.scalacheck.{Gen, Prop}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class EncoderSpec extends AnyWordSpec with Checkers with TestEncoder with TestTokenStream {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20000)

  implicit val eq: Eq[StreamingEncoderError] = Eq.by(_.toString())

  "Encoder" should {
    "encode json" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { string =>
            testEncoder(string, LazyList(Right(JsonToken.Str(string))))
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { i =>
            testEncoder(i, LazyList(Right(JsonToken.Number(i))))
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { b =>
            testEncoder(b, LazyList(Right(JsonToken.Boolean(b))))
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { bigDecimal =>
            testEncoder(bigDecimal, LazyList(Right(JsonToken.Number(bigDecimal))))
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { localDate =>
            testEncoder(localDate, LazyList(Right(JsonToken.Str(localDate.toString()))))
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testEncoder(
              ints,
              Right(JsonToken.BeginArray) #:: LazyList.from(ints).map(i => Right(JsonToken.Number(i))) ++ LazyList(
                Right(JsonToken.EndArray)
              )
            )
          }
        )
      }

      "value is a very long List (stack safety)" in {
        check {
          val range = LazyList.range(1, 100000)

          val result = implicitly[Encoder[List[Int]]].encode(range.toList)

          val expected: StreamingEncoderResult =
            Right(JsonToken.BeginArray) #:: range.map(n => Right(JsonToken.Number(n))) #::: LazyList(
              Right(JsonToken.EndArray)
            )

          ((result.eqv(expected)): Prop) //:| s"${result.map(_.length)} vs ${expected.map(_.length)}"
        }
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testEncoder(
              maybeInt,
              maybeInt.fold[StreamingEncoderResult](LazyList(Right(JsonToken.Null)))(i =>
                LazyList(Right(JsonToken.Number(i)))
              )
            )
          }
        )
      }

      "value is a map" ignore {
        //todo!
      }

      "value is a large map (stack safety)" in {
        check {
          val range = LazyList.range(1, 100000)
          val map = range.foldLeft(Map.empty[String, Int]) { case (m, i) => m + (i.toString() -> i) }

          val result = implicitly[Encoder[Map[String, Int]]].encode(map)

          val expected: StreamingEncoderResult =
            Right(JsonToken.BeginObject) #:: (range.flatMap(n =>
              Right(JsonToken.Key(n.toString)) #:: Right(JsonToken.Number(n)) #:: LazyList.empty
            ) ++ LazyList(Right(JsonToken.EndObject)))

          ((result.toSet.diff(expected.toSet).eqv(Set.empty)): Prop) :| s"result $result \nvs expected: $expected"
        }
      }
    }
  }
}
