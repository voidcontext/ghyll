package ghyll

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.instances.byte._
import cats.instances.list._
import cats.syntax.eq._
import ghyll.Utils._
import org.scalacheck.Prop

trait TestEncoder {
  def testEncoder[A](value: A, expected: String)(implicit encoder: Encoder[A]) = {
    val out = new ByteArrayOutputStream()

    createWriter(Resource.fromAutoCloseable(IO.delay(out)))
      .use(writer => IO.delay(encoder.encode(writer, value)))
      .map { result =>
        val encoded = new String(out.toByteArray(), StandardCharsets.UTF_16)
        (result.isRight: Prop) :| s"$result should be Right(Unit)" &&
        (out.toByteArray().toList === expected.getBytes(StandardCharsets.UTF_16).toList: Prop) :|
          s"""encoded value: "${encoded}"
             |doesn't equal expected: "${expected}
             |encoded bytes: ${byteArrayToString(out.toByteArray())}
             |expevcted bytes: ${byteArrayToString(expected.getBytes(StandardCharsets.UTF_16))}"""".stripMargin
      }
      .unsafeRunSync()
  }

  def byteArrayToString(ba: Array[Byte]): String =
    ba.toList.map(_.toInt).toString()
}
