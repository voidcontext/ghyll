package ghyll

// import java.io.{File, FileInputStream}

// import cats.effect.IO
// import cats.effect.unsafe.implicits.global
// import ghyll.auto.semi._
// import ghyll.jsonpath._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DecodeSpec extends AnyWordSpec with Matchers {
  // case class Foo(bar: String, baz: Int)
  // implicit val fooDecoder: Decoder[Foo] = deriveDecoder

  // case class FooList(bar: List[Int])
  // implicit val fooListDecoder: Decoder[FooList] = deriveDecoder

  // case class FooOption(bar: Option[String], baz: Int)
  // implicit val fooOptionDecoder: Decoder[FooOption] = deriveDecoder

  // case class Data(value: BigDecimal, additional: Option[List[String]])
  // implicit val dataDecoder: Decoder[Data] = deriveDecoder
  // case class Obj(name: String, bool: Option[Boolean], data: Option[Data])
  // implicit val objDecoder: Decoder[Obj] = deriveDecoder

  // val fileSimple = new File("modules/core/src/test/resources/test-object-simple.json")
  // val fileNested = new File("modules/core/src/test/resources/test-object.json")

  // "decodeObject()" should {

  //   "only decode the object under the given path" in {
  //     decodeObject[IO, Obj]("data" >:: "bar-2" >:: JNil, new FileInputStream(fileNested)).use { result =>
  //       IO.delay(
  //         result should be(Right(Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))))
  //       )
  //     }
  //   }

  //   "provide a shortcut to decode from file" in {
  //     decodeObject[IO, Obj]("data" >:: "bar-2" >:: JNil, fileNested).use { result =>
  //       IO.delay(
  //         result should be(Right(Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))))
  //       )
  //     }
  //   }
  // }

  // "decodeKeyValues" should {

  //   "stream keys and values" in {
  //     val expected =
  //       List(
  //         Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
  //         Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
  //         Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
  //         Right("baz" -> Obj("baz", None, None))
  //       )

  //     decodeKeyValues[IO, Obj](new FileInputStream(fileSimple))
  //       .use(
  //         _.compile.toList.map {
  //           _ should be(expected)
  //         }
  //       )
  //       .unsafeRunSync()
  //   }

  //   "only decode the object under the given path" in {

  //     val expected =
  //       List(
  //         Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
  //         Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
  //         Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
  //         Right("baz" -> Obj("baz", None, None))
  //       )

  //     decodeKeyValues[IO, Obj]("data" >:: JNil, new FileInputStream(fileNested))
  //       .use(
  //         _.compile.toList.map {
  //           _ should be(expected)
  //         }
  //       )
  //       .unsafeRunSync()
  //   }

  //   "provide a shortcut to decode from file" in {

  //     val expected =
  //       List(
  //         Right("foo" -> Obj("foo", Some(false), Some(Data(BigDecimal.valueOf(1L), None)))),
  //         Right("bar" -> Obj("bar", None, Some(Data(BigDecimal.valueOf(1.9), Some(List.empty))))),
  //         Right("bar-2" -> Obj("bar2", None, Some(Data(BigDecimal.valueOf(1.9), Some(List("1", "2", "3")))))),
  //         Right("baz" -> Obj("baz", None, None))
  //       )

  //     decodeKeyValues[IO, Obj]("data" >:: JNil, fileNested)
  //       .use(
  //         _.compile.toList.map {
  //           _ should be(expected)
  //         }
  //       )
  //       .unsafeRunSync()
  //   }
  // }
}
