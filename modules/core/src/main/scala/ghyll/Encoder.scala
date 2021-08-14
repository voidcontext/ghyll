package ghyll

import java.time.LocalDate

// import fs2.Stream
import ghyll.json.JsonToken

trait Encoder[A] {
  type For = A

  def encode(value: A): StreamingEncoderResult
}

object Encoder {
  def apply[A](implicit ev: Encoder[A]) = ev

  implicit def stringEncoder: Encoder[String] =
    (value) => Right(LazyList(JsonToken.Str(value)))

  implicit def intEncoder: Encoder[Int] =
    (value) => Right(LazyList(JsonToken.Number(value)))

  implicit def booleanEncoder: Encoder[Boolean] =
    (value) => Right(LazyList(JsonToken.Boolean(value)))

  implicit def bigDecimalEncoder: Encoder[BigDecimal] =
    (value) => Right(LazyList(JsonToken.Number(value)))

  implicit def localDateEncoder: Encoder[LocalDate] =
    (value) => Right(LazyList(JsonToken.Str(value.toString())))

  implicit def optionEncoder[F[_], A](implicit encoder: Encoder[A]): Encoder[Option[A]] =
    (value) => value.fold[StreamingEncoderResult](Right(LazyList(JsonToken.Null)))(encoder.encode)

  implicit def listEncoder[F[_], A](implicit encoder: Encoder[A]): Encoder[List[A]] =
    (value) =>
      value
        .foldLeft[StreamingEncoderResult](Right(LazyList[JsonToken](JsonToken.BeginArray))) { (result, v) =>
          result.flatMap(rs => encoder.encode(v).map(rs ++ _))
        }
        .map(_ ++ LazyList(JsonToken.EndArray))

  implicit def mapEncoder[F[_], A](implicit valueEncoder: Encoder[A]): Encoder[Map[String, A]] =
    (value) =>
      value
        .foldLeft[StreamingEncoderResult](Right(LazyList[JsonToken](JsonToken.BeginObject))) { case (result, (k, v)) =>
          result.flatMap(rs => valueEncoder.encode(v).map(rs ++ LazyList(JsonToken.Key(k)) ++ _))
        }
        .map(_ ++ LazyList(JsonToken.EndObject))

}
