package ghyll.syntax

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ghyll.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SyntaxSpec extends AnyWordSpec with Matchers {
  case class Foo(bar: String)
  case class WrappedFoo(foo: Foo)

  case class Response[A](data: A)

  "asJsonString" should {
    "convert the given value to json string" in {
      val json = Response(WrappedFoo(Foo("baz"))).asJsonString[IO].unsafeRunSync()

      json should be("""{"data":{"foo":{"bar":"baz"}}}""")
    }
  }
}
