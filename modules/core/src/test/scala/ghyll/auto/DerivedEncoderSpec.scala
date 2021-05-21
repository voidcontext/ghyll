package ghyll.auto

import ghyll._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DerivedEncoderSpec extends AnyWordSpec with Matchers with TestEncoder {

  // case class Foo(bar: String, baz: Int)
  // val fooDecoder: DerivedEncoder[Foo] = implicitly[DerivedEncoder[Foo]]

  // case class FooOption(bar: Option[String], baz: Int)
  // val fooOptionDecoder: DerivedEncoder[FooOption] = implicitly[DerivedEncoder[FooOption]]

  // case class Root(foo: Foo)

  // "DerivedEncoder.encode" should {
  //   "encode case classes" when {
  //     "there are simple scalar attributes" in {
  //       testEncoder(Foo("foobar", 42), """{"bar": "foobar","baz":"42}""")(fooDecoder)
  //     }

  //     "there are provided optional attributes" in {
  //       testEncoder(FooOption(Some("foobar"), 42), """{"bar": "foobar","baz":42}""")(fooOptionDecoder)
  //     }

  //     "there are not provided optional attributes" in {
  //       testEncoder(FooOption(None, 42), """{"baz":42}""")(fooOptionDecoder)
  //     }
  //   }
  // }
}
