package ghyll

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

import cats.effect.{IO, Resource}
import ghyll.Generators._
import ghyll.Utils.createWriter
import org.scalacheck.Prop
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
class EncoderSpec extends AnyWordSpec with Checkers {
  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20000)

  def testEncoder[A](value: A, expected: String)(implicit encoder: Encoder[A]) = {
    val out = new ByteArrayOutputStream()
    createWriter(Resource.fromAutoCloseable(IO.delay(out)))
      .use(writer => IO.delay(encoder.encode(writer, value)))
      .map { result =>
        val encoded = new String(out.toByteArray(), StandardCharsets.UTF_16)
        (result == Right(()): Prop) :| s"$result should be Right(Unit)" &&
        (out.toByteArray().toList == expected.getBytes(StandardCharsets.UTF_16).toList: Prop) :|
          s"""encoded value: "${encoded}" 
             |doesn't equal expected: "${expected}
             |encoded bytes: ${byteArrayToString(out.toByteArray())}
             |expevcted bytes: ${byteArrayToString(expected.getBytes(StandardCharsets.UTF_16))}"""".stripMargin
      }
      .unsafeRunSync()

  }

  def byteArrayToString(ba: Array[Byte]): String =
    ba.toList.map(_.toInt).toString()

  def escape(str: String): String =
    """\p{Cntrl}""".r
      .replaceAllIn(
        str.replaceAll("\\\\", """\\\\"""),
        { m =>
          m.matched
            .codePoints()
            .toArray()
            .toList
            .map {
              case bs if bs == 8  => "\\\\b"
              case bs if bs == 9  => "\\\\t"
              case bs if bs == 10 => "\\\\n"
              case bs if bs == 12 => "\\\\f"
              case bs if bs == 13 => "\\\\r"
              case i if i == 127  => i.toChar
              case i              => "\\\\u%04x".format(i)
            }
            .mkString
        }
      )
      .replaceAll("\"", "\\\\\"")
      .replaceAll("\u2028", "\\\\u2028")
      .replaceAll("\u2029", "\\\\u2029")

  "Encoder" should {
    "encode json" when {
      "value is a String" in {
        check(
          Prop.forAll(string) { string =>
            testEncoder(Map("foo" -> string), s"""{"foo":"${escape(string)}"}""")
          }
        )
      }
    }
  }
}
