package ghyll

import ghyll.Generators._
import ghyll.Utils.escape
import org.scalacheck.{Gen, Prop}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
class EncoderSpec extends AnyWordSpec with Checkers with TestEncoder {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20000)

  "Encoder" should {
    "encode json" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { string =>
            testEncoder(Map("foo" -> string), s"""{"foo":"${escape(string)}"}""")
          }
        )
      }

      "value is an Int" in {
        check(
          Prop.forAll(int) { i =>
            testEncoder(Map("foo" -> i), s"""{"foo":${i.toString()}}""")
          }
        )
      }

      "value is a Boolean" in {
        check(
          Prop.forAll(boolean) { b =>
            testEncoder(Map("foo" -> b), s"""{"foo":${if (b) "true" else "false"}}""")
          }
        )
      }

      "value is a BigDecimal" in {
        check(
          Prop.forAll(bigDecimal) { case (bigDecimal) =>
            testEncoder(Map("foo" -> bigDecimal), s"""{"foo":${bigDecimal.toString()}}""")
          }
        )
      }

      "value is a LocalDate" in {
        check(
          Prop.forAll(localDate) { case (localDate) =>
            testEncoder(Map("foo" -> localDate), s"""{"foo":"${localDate.toString()}"}""")
          }
        )
      }

      "value is a List" in {
        check(
          Prop.forAll(Gen.listOf(int)) { case (ints) =>
            testEncoder(Map("foo" -> ints), s"""{"foo":[${ints.map(_.toString()).mkString(",")}]}""")
          }
        )
      }

      "value is optional" in {
        check(
          Prop.forAll(Gen.option(int)) { case (maybeInt) =>
            testEncoder(Map("foo" -> maybeInt), s"""{"foo":${maybeInt.fold("null")(_.toString())}}""")
          }
        )
      }

    }
  }
}
