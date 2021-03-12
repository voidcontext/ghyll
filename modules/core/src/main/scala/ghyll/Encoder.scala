package ghyll

import java.time.LocalDate

import cats.instances.either._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.gson.stream.JsonWriter

trait Encoder[A] {
  def encode(writer: JsonWriter, value: A): StreamingEncoderResult
}

object Encoder {
  implicit val stringEncoder: Encoder[String] =
    (writer, value) => catchNonFatal(writer.value(value))

  implicit val intEncoder: Encoder[Int] =
    (writer, value) => catchNonFatal(writer.value(BigInt(value)))

  implicit val booleanEncoder: Encoder[Boolean] =
    (writer, value) => catchNonFatal(writer.value(value))

  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    (writer, value) => catchNonFatal(writer.value(value))

  implicit val localDateEncoder: Encoder[LocalDate] =
    (writer, value) => catchNonFatal(writer.value(value.toString()))

  implicit def optionEncoder[A](implicit encoder: Encoder[A]): Encoder[Option[A]] =
    (writer, value) => value.fold(catchNonFatal(writer.nullValue()))(encoder.encode(writer, _))

  implicit def listEncoder[A](implicit encoder: Encoder[A]): Encoder[List[A]] =
    (writer, value) =>
      value.foldLeft(catchNonFatal(writer.beginArray())) { (result, elem) =>
        result >> encoder.encode(writer, elem)
      } >> catchNonFatal(writer.endArray())

  implicit def mapEncoder[A](implicit valueEncoder: Encoder[A]): Encoder[Map[String, A]] =
    (writer, value) => {
      value.foldLeft(catchNonFatal(writer.beginObject())) { case (result, (k, v)) =>
        result >> catchNonFatal(writer.name(k)) >> valueEncoder.encode(writer, v)
      } >> catchNonFatal(writer.endObject())
    }

  private def catchNonFatal[A](body: => A): StreamingEncoderResult =
    Either
      .catchNonFatal(body)
      .left
      .map(t => StreamingEncodingFailure(t.getMessage()))
      .void
}
