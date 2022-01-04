package ghyll

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.instances.list._
import cats.kernel.Eq
import cats.syntax.eq._
import ghyll.Generators._
import ghyll.json.{JsonToken, TestJsonTokenWriter}
import org.scalacheck.{Gen, Prop}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class EncoderSpec extends AnyWordSpec with Checkers with TestEncoder {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20000)

  implicit val eq: Eq[StreamingEncoderError] = Eq.by(_.toString())

  "Encoder" should {
    "encode json" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { string =>
            testEncoder(string, List(JsonToken.Str(string)))
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { i =>
            testEncoder(i, List((JsonToken.Number(i))))
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { b =>
            testEncoder(b, List((JsonToken.Boolean(b))))
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { bigDecimal =>
            testEncoder(bigDecimal, List((JsonToken.Number(bigDecimal))))
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { localDate =>
            testEncoder(localDate, List((JsonToken.Str(localDate.toString()))))
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testEncoder(
              ints,
              (JsonToken.BeginArray) :: ints.map(i => (JsonToken.Number(i))) ++ List(
                (JsonToken.EndArray)
              )
            )
          }
        )
      }

      "value is a very long List (stack safety)" in {
        check {
          val writer = TestJsonTokenWriter[IO]

          val range = List.range(1, 100000)

          implicitly[Encoder[IO, List[Int]]].encode(range.toList, writer).unsafeRunSync()

          val expected: List[JsonToken] =
            JsonToken.BeginArray :: range.map(n => JsonToken.Number(n)) ++ List(
              JsonToken.EndArray
            )

          ((writer.written
            .unsafeRunSync()
            .eqv(expected)): Prop) //:| s"${result.map(_.length)} vs ${expected.map(_.length)}"
        }
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testEncoder(
              maybeInt,
              maybeInt.fold[List[JsonToken]](List(JsonToken.Null))(i => List(JsonToken.Number(i)))
            )
          }
        )
      }

      "value is a map" ignore {
        //todo!
      }

      "value is a large map (stack safety)" in {
        check {
          val writer = TestJsonTokenWriter[IO]
          val range = List.range(1, 100000)
          val map = range.foldLeft(Map.empty[String, Int]) { case (m, i) => m + (i.toString() -> i) }

          implicitly[Encoder[IO, Map[String, Int]]].encode(map, writer).unsafeRunSync()

          val expected: List[JsonToken] =
            JsonToken.BeginObject :: (range.flatMap(n =>
              JsonToken.Key(n.toString) :: (JsonToken.Number(n)) :: List.empty
            ) ++ List(JsonToken.EndObject))

          val result = writer.written.unsafeRunSync()
          ((result.toSet.diff(expected.toSet).eqv(Set.empty)): Prop) :| s"result $result \nvs expected: $expected"
        }
      }
    }
  }
}
