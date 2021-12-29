package ghyll

import java.time.LocalDate

import ghyll.json.JsonTokenReader

trait Decoder[F[_], A] {
  def decode(reader: JsonTokenReader[F]): StreamingDecoderResult[F, A]
}

object Decoder {
  def apply[F[_], A](implicit ev: Decoder[F, A]) = ev

  implicit def stringDecoder[F[_]]: Decoder[F, String] = ???

  implicit def intDecoder[F[_]]: Decoder[F, Int] = ???

  implicit def booleanDecoder[F[_]]: Decoder[F, Boolean] = ???

  implicit def bigDecimalDecoder[F[_]]: Decoder[F, BigDecimal] = ???

  implicit def localDateDecoder[F[_]]: Decoder[F, LocalDate] = ???

  implicit def optionDecoder[F[_], A](implicit aDecoder: Decoder[F, A]): Decoder[F, Option[A]] = ???

  implicit def listDecoder[F[_], A](implicit aDecoder: Decoder[F, A]): Decoder[F, List[A]] = ???

  implicit def mapDecoder[F[_], V](implicit valueDecoder: Decoder[F, V]): Decoder[F, Map[String, V]] = ???

}
