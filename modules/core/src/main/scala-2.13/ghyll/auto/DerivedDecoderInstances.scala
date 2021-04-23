package ghyll.auto

import scala.annotation.tailrec

import cats.instances.string._
import cats.syntax.eq._
import com.google.gson.stream.{JsonReader, JsonToken}
import ghyll.StreamingDecoderError
import ghyll.gson.Implicits._
import shapeless._

private[ghyll] trait DerivedDecoderInstances {
  implicit def derivedDecoderGeneric[A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[A],
    mapper: ReprMapper[H]
  ): DerivedDecoder[A] =
    reader => {
      reader.beginObject()
      val hlist = decodeKeys(reader, Right(Map.empty), fieldDecoders)
      skipRemainingKeys(reader)
      reader.endObject()

      hlist.flatMap(mapper.fromMap(_)).map(lg.from(_))
    }

  @tailrec
  private[this] def decodeKeys[A](
    reader: JsonReader,
    decodedFields: Either[StreamingDecoderError, Map[String, Any]],
    fieldDecoders: FieldDecoder[A]
  ): Either[StreamingDecoderError, Map[String, Any]] =
    if (reader.peek() === JsonToken.END_OBJECT) decodedFields
    else {
      val name = reader.nextName()
      fieldDecoders.fields
        .find(_.name === name) match {
        case None        =>
          reader.skipValue()
          decodeKeys(reader, decodedFields, fieldDecoders)
        case Some(field) =>
          decodeKeys(
            reader,
            for {
              m       <- decodedFields
              decoded <- field.decoder.decode(reader)
            } yield m + (name -> decoded),
            fieldDecoders
          )
      }
    }

  @SuppressWarnings(Array("scalafix:DisableSyntax.while"))
  @inline private[this] def skipRemainingKeys(reader: JsonReader): Unit =
    while (reader.peek() === JsonToken.NAME) {
      reader.nextName()
      reader.skipValue()
    }
}
