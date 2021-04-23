package ghyll

import java.time.LocalDate

import cats.instances.either._
import cats.syntax.flatMap._
import com.google.gson.stream.JsonWriter
import ghyll.StreamingEncoderResult.catchEncodingFailure

trait Encoder[A] {
  type For = A

  def encode(writer: JsonWriter, value: A): StreamingEncoderResult
}

object Encoder {
  implicit val stringEncoder: Encoder[String] =
    (writer, value) => catchEncodingFailure(writer.value(value))

  implicit val intEncoder: Encoder[Int] =
    (writer, value) => catchEncodingFailure(writer.value(BigInt(value)))

  implicit val booleanEncoder: Encoder[Boolean] =
    (writer, value) => catchEncodingFailure(writer.value(value))

  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    (writer, value) => catchEncodingFailure(writer.value(value))

  implicit val localDateEncoder: Encoder[LocalDate] =
    (writer, value) => catchEncodingFailure(writer.value(value.toString()))

  implicit def optionEncoder[A](implicit encoder: Encoder[A]): Encoder[Option[A]] =
    (writer, value) => value.fold(catchEncodingFailure(writer.nullValue()))(encoder.encode(writer, _))

  implicit def listEncoder[A](implicit encoder: Encoder[A]): Encoder[List[A]] =
    (writer, value) =>
      value.foldLeft(catchEncodingFailure(writer.beginArray())) { (result, elem) =>
        result >> encoder.encode(writer, elem)
      } >> catchEncodingFailure(writer.endArray())

  implicit def mapEncoder[A](implicit valueEncoder: Encoder[A]): Encoder[Map[String, A]] =
    (writer, value) => {
      value.foldLeft(catchEncodingFailure(writer.beginObject())) { case (result, (k, v)) =>
        result >> catchEncodingFailure(writer.name(k)) >> valueEncoder.encode(writer, v)
      } >> catchEncodingFailure(writer.endObject())
    }

}
