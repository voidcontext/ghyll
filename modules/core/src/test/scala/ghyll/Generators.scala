package ghyll

import java.time.{LocalDate, ZoneId}

import org.scalacheck.{Arbitrary, Gen}

object Generators {
  val string: Gen[String] = Gen.oneOf(Gen.asciiStr, Arbitrary.arbString.arbitrary)
  val int: Gen[Int] = Arbitrary.arbInt.arbitrary
  val boolean: Gen[Boolean] = Gen.oneOf(false, true)
  val bigDecimal: Gen[BigDecimal] = Arbitrary.arbBigDecimal.arbitrary
  val localDate: Gen[LocalDate] =
    Arbitrary.arbDate.arbitrary.map(_.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())

  val mapOfDates: Gen[Map[String, LocalDate]] =
    Gen.mapOf(
      for {
        k <- string
        v <- localDate
      } yield (k, v)
    )
}
