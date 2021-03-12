package ghyll

import cats.instances.either._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.gson.stream.JsonWriter

trait Encoder[A] {
  def encode(writer: JsonWriter, value: A): StreamingEncoderResult
}

object Encoder {
  implicit def stringEncoder: Encoder[String] =
    (writer, value) => catchNonFatal(writer.value(value))

  implicit def mapEncoder[A](implicit valueEncoder: Encoder[A]): Encoder[Map[String, A]] =
    (writer, value) => {
      value.foldLeft[StreamingEncoderResult](catchNonFatal(writer.beginObject())) { case (result, (k, v)) =>
        result >> catchNonFatal(writer.name(k)) >> valueEncoder.encode(writer, v)
      } >> catchNonFatal(writer.endObject())
    }

  private def catchNonFatal[A](body: A): StreamingEncoderResult =
    Either
      .catchNonFatal(body)
      .left
      .map(t => StreamingEncodingFailure(t.getMessage()))
      .void
}
