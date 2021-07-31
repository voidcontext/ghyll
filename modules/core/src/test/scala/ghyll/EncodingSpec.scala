package ghyll

import java.io.ByteArrayOutputStream

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ghyll.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EncodingSpec extends AnyWordSpec with Matchers with Encoding {
  case class Foo(bar: String, baz: Int)

  "encode()" should {
    "encode and write the JSON into the given stream" in {
      val out = new ByteArrayOutputStream()

      encode[IO, Foo](Foo("this is a string", 42), out).flatMap { result =>
        IO.delay {
          result should be(Right(()))
        }
      }
        .unsafeRunSync()

      out.toString() should be("""{"bar":"this is a string","baz":42}""")
    }
  }
}
