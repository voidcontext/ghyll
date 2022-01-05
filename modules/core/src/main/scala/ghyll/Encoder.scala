package ghyll

import java.time.LocalDate

import cats.data.EitherT
import cats.instances.either._
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.{Functor, Monad}
import ghyll.json.{JsonToken, JsonTokenWriter}
import ghyll.utils.EitherOps

trait Encoder[F[_], A] {
  type For = A

  def encode(value: A, writer: JsonTokenWriter[F]): StreamingEncoderResult[F]
}

object Encoder {
  def apply[F[_], A](implicit ev: Encoder[F, A]) = ev

  implicit def stringEncoder[F[_]: Functor]: Encoder[F, String] =
    (value, writer) => writer.write(JsonToken.Str(value)).map(_.right)

  implicit def intEncoder[F[_]: Functor]: Encoder[F, Int] =
    (value, writer) => writer.write(JsonToken.Number(value)).map(_.right)

  implicit def booleanEncoder[F[_]: Functor]: Encoder[F, Boolean] =
    (value, writer) => writer.write(JsonToken.Boolean(value)).map(_.right)

  implicit def bigDecimalEncoder[F[_]: Functor]: Encoder[F, BigDecimal] =
    (value, writer) => writer.write(JsonToken.Number(value)).map(_.right)

  implicit def localDateEncoder[F[_]: Functor]: Encoder[F, LocalDate] =
    (value, writer) => writer.write(JsonToken.Str(value.toString())).map(_.right)

  implicit def optionEncoder[F[_]: Functor, A](implicit encoder: Encoder[F, A]): Encoder[F, Option[A]] =
    (value, writer) =>
      value.fold(writer.write(JsonToken.Null).map(_.right[StreamingEncoderError]))(encoder.encode(_, writer))

  implicit def listEncoder[F[_]: Monad, A](implicit encoder: Encoder[F, A]): Encoder[F, List[A]] =
    (value, writer) =>
      (EitherT(writer.write(JsonToken.BeginArray).map(_.right[StreamingEncoderError])) >> EitherT(
        value.traverse(v => encoder.encode(v, writer)).map(_.sequence.as(()))
      ) >> EitherT(writer.write(JsonToken.EndArray).map(_.right[StreamingEncoderError]))).value

  implicit def mapEncoder[F[_]: Monad, A](implicit valueEncoder: Encoder[F, A]): Encoder[F, Map[String, A]] =
    (values, writer) =>
      (EitherT(writer.write(JsonToken.BeginObject).map(_.right[StreamingEncoderError])) >> EitherT(
        values.toList.traverse { case (key, value) =>
          writer.write(JsonToken.Key(key)) >>
            valueEncoder.encode(value, writer)
        }.map(_.sequence.as(()))
      ) >> EitherT(writer.write(JsonToken.EndObject).map(_.right[StreamingEncoderError]))).value
}
