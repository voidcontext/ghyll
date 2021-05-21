package ghyll

import java.time.LocalDate

import fs2.Stream
import ghyll.json.JsonToken

trait Encoder[F[_], A] {
  type For = A

  def encode(stream: Stream[F, JsonToken], value: A): StreamingEncoderResult[F]
}

object Encoder extends EncoderInstances {
  def apply[F[_], A](implicit ev: Encoder[F, A]) = ev

  implicit def stringEncoder[F[_]]: Encoder[F, String] =
    (stream, value) => Right(stream ++ Stream.emit(JsonToken.Str(value)))

  implicit def intEncoder[F[_]]: Encoder[F, Int] =
    (stream, value) => Right(stream ++ Stream.emit(JsonToken.Number(value.toString())))

  implicit def booleanEncoder[F[_]]: Encoder[F, Boolean] =
    (stream, value) => Right(stream ++ Stream.emit(JsonToken.Boolean(value)))

  implicit def bigDecimalEncoder[F[_]]: Encoder[F, BigDecimal] =
    (stream, value) => Right(stream ++ Stream.emit(JsonToken.Str(value.toString())))

  implicit def localDateEncoder[F[_]]: Encoder[F, LocalDate] =
    (stream, value) => Right(stream ++ Stream.emit(JsonToken.Str(value.toString())))

  implicit def optionEncoder[F[_], A](implicit encoder: Encoder[F, A]): Encoder[F, Option[A]] =
    (stream, value) =>
      value.fold[StreamingEncoderResult[F]](Right(stream ++ (Stream.emit(JsonToken.Null))))(encoder.encode(stream, _))

  implicit def listEncoder[F[_], A](implicit encoder: Encoder[F, A]): Encoder[F, List[A]] =
    (stream, value) =>
      value
        .foldLeft[StreamingEncoderResult[F]](Right(Stream.emit[F, JsonToken](JsonToken.BeginArray))) { (result, v) =>
          result.flatMap(rs => encoder.encode(rs, v))
        }
        .map(s => stream ++ s ++ Stream.emit(JsonToken.EndArray))

  implicit def mapEncoder[F[_], A](implicit valueEncoder: Encoder[F, A]): Encoder[F, Map[String, A]] =
    (stream, value) =>
      value
        .foldLeft[StreamingEncoderResult[F]](Right(Stream.emit[F, JsonToken](JsonToken.BeginObject))) {
          case (result, (k, v)) =>
            result.flatMap(rs => valueEncoder.encode(rs ++ Stream.emit(JsonToken.Key(k)), v))
        }
        .map(s => stream ++ s ++ Stream.emit(JsonToken.EndArray))

}
