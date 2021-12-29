package ghyll

import java.time.LocalDate

import ghyll.json.JsonTokenWriter

trait Encoder[F[_], A] {
  type For = A

  def encode(value: A, writer: JsonTokenWriter[F]): StreamingEncoderResult[F]
}

object Encoder {
  def apply[F[_], A](implicit ev: Encoder[F, A]) = ev

  implicit def stringEncoder[F[_]]: Encoder[F, String] = ???

  implicit def intEncoder[F[_]]: Encoder[F, Int] = ???

  implicit def booleanEncoder[F[_]]: Encoder[F, Boolean] = ???

  implicit def bigDecimalEncoder[F[_]]: Encoder[F, BigDecimal] = ???

  implicit def localDateEncoder[F[_]]: Encoder[F, LocalDate] = ???

  implicit def optionEncoder[F[_], A](implicit encoder: Encoder[F, A]): Encoder[F, Option[A]] = ???

  implicit def listEncoder[F[_], A](implicit encoder: Encoder[F, A]): Encoder[F, List[A]] =  ???

  implicit def mapEncoder[F[_], A](implicit valueEncoder: Encoder[F, A]): Encoder[F, Map[String, A]] = ???
}
