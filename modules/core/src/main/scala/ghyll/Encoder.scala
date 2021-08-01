package ghyll

import java.time.LocalDate

import fs2.Stream
import ghyll.json.JsonToken

trait Encoder[F[_], A] {
  type For = A

  def encode(value: A): StreamingEncoderResult[F]
}

object Encoder {
  def apply[F[_], A](implicit ev: Encoder[F, A]) = ev

  implicit def stringEncoder[F[_]]: Encoder[F, String] =
    (value) => Right(Stream.emit(JsonToken.Str(value)))

  implicit def intEncoder[F[_]]: Encoder[F, Int] =
    (value) => Right(Stream.emit(JsonToken.Number(value)))

  implicit def booleanEncoder[F[_]]: Encoder[F, Boolean] =
    (value) => Right(Stream.emit(JsonToken.Boolean(value)))

  implicit def bigDecimalEncoder[F[_]]: Encoder[F, BigDecimal] =
    (value) => Right(Stream.emit(JsonToken.Number(value)))

  implicit def localDateEncoder[F[_]]: Encoder[F, LocalDate] =
    (value) => Right(Stream.emit(JsonToken.Str(value.toString())))

  implicit def optionEncoder[F[_], A](implicit encoder: Encoder[F, A]): Encoder[F, Option[A]] =
    (value) => value.fold[StreamingEncoderResult[F]](Right(Stream.emit(JsonToken.Null)))(encoder.encode)

  implicit def listEncoder[F[_], A](implicit encoder: Encoder[F, A]): Encoder[F, List[A]] =
    (value) =>
      value
        .foldLeft[StreamingEncoderResult[F]](Right(Stream.emit[F, JsonToken](JsonToken.BeginArray))) { (result, v) =>
          result.flatMap(rs => encoder.encode(v).map(rs ++ _))
        }
        .map(_ ++ Stream.emit(JsonToken.EndArray))

  implicit def mapEncoder[F[_], A](implicit valueEncoder: Encoder[F, A]): Encoder[F, Map[String, A]] =
    (value) =>
      value
        .foldLeft[StreamingEncoderResult[F]](Right(Stream.emit[F, JsonToken](JsonToken.BeginObject))) {
          case (result, (k, v)) =>
            result.flatMap(rs => valueEncoder.encode(v).map(rs ++ Stream.emit(JsonToken.Key(k)) ++ _))
        }
        .map(_ ++ Stream.emit(JsonToken.EndArray))

}
