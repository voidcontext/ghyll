package ghyll

import java.time.LocalDate

import ghyll.json.JsonToken

trait Encoder[A] {
  type For = A

  def encode(value: A): StreamingEncoderResult
}

object Encoder {
  def apply[A](implicit ev: Encoder[A]) = ev

  implicit def stringEncoder: Encoder[String] =
    (value) => LazyList(Right(JsonToken.Str(value)))

  implicit def intEncoder: Encoder[Int] =
    (value) => LazyList(Right(JsonToken.Number(value)))

  implicit def booleanEncoder: Encoder[Boolean] =
    (value) => LazyList(Right(JsonToken.Boolean(value)))

  implicit def bigDecimalEncoder: Encoder[BigDecimal] =
    (value) => LazyList(Right(JsonToken.Number(value)))

  implicit def localDateEncoder: Encoder[LocalDate] =
    (value) => LazyList(Right(JsonToken.Str(value.toString())))

  implicit def optionEncoder[F[_], A](implicit encoder: Encoder[A]): Encoder[Option[A]] =
    (value) => value.fold[StreamingEncoderResult](LazyList(Right(JsonToken.Null)))(encoder.encode)

  implicit def listEncoder[F[_], A](implicit encoder: Encoder[A]): Encoder[List[A]] = {
    def next(xs: List[A]): StreamingEncoderResult =
      Left(InternalResult) #:: (xs match {
        case head :: tail => encoder.encode(head) ++ next(tail).tail
        case Nil          => LazyList(Right(JsonToken.EndArray))
      })

    (value) => Right(JsonToken.BeginArray) #:: next(value).tail
  }

  implicit def mapEncoder[F[_], A](implicit valueEncoder: Encoder[A]): Encoder[Map[String, A]] = {
    def next(xs: List[(String, A)]): StreamingEncoderResult =
      Left(InternalResult) #:: (xs match {
        case (key, value) :: tail => Right(JsonToken.Key(key)) #:: valueEncoder.encode(value) ++ next(tail).tail
        case Nil                  => LazyList(Right(JsonToken.EndObject))
      })

    (value) => Right(JsonToken.BeginObject) #:: next(value.toList).tail
  }
}
