package com.gaborpihaj.jsonstream.v2

import java.io.{InputStream, InputStreamReader}

import scala.annotation.tailrec

import cats.effect.{Resource, Sync}
import com.google.gson.stream.{JsonReader, JsonToken}
import fs2.Stream
import shapeless._

@SuppressWarnings(Array("scalafix:DisableSyntax.==", "scalafix:DisableSyntax.while"))
object StreamingDecoder2 {

  trait StreamingDecoder[F[_]] {
    def decodeArray[T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[T]]]
    def decodeObject[T: Decoder](jaon: InputStream): Resource[F, StreamingDecoderResult[T]]
    def decodeKeyValues[T: Decoder](
      json: InputStream
    ): Resource[F, Stream[F, StreamingDecoderResult[
      (String, T)
    ]]] // TODO: make this more generic: StreamingDecoderResult[(K, V)]
  }

  def decoder[F[_]: Sync]: StreamingDecoder[F] =
    new StreamingDecoder[F] {

      def decodeArray[T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[T]]] = ???

      def decodeObject[T: Decoder](json: InputStream): Resource[F, StreamingDecoderResult[T]] =
        readerResource(json).map { reader =>
          if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            Decoder[T].decode(reader)
          } else Left(StreamingDecodingFailure("Not an object!"))
        }

      def decodeKeyValues[T](
        json: InputStream
      )(implicit d: Decoder[T]): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
        readerResource(json).map { reader =>
          Stream
            .eval(Sync[F].delay { reader.beginObject(); reader })
            .flatMap(
              Stream.unfold(_) { reader =>
                if (reader.peek() == JsonToken.END_OBJECT) None
                else {
                  val key = reader.nextName()
                  Option(d.decode(reader).map(key -> _) -> reader)
                }
              }
            )
        }

      private def readerResource(json: InputStream): Resource[F, JsonReader] =
        Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(json))))

    }

  def deriveDecoder[A, H <: HList](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[A],
    mapper: ReprMapper[H]
  ): Decoder[A] =
    reader => {

      @tailrec
      def decodeKeys(
        reader: JsonReader,
        decodedFields: Either[StreamingDecoderError, Map[String, Any]]
      ): Either[StreamingDecoderError, Map[String, Any]] =
        if (reader.peek() == JsonToken.END_OBJECT) decodedFields
        else {
          val name = reader.nextName()
          fieldDecoders.fields
            .find(_.name == name) match {
            case None        =>
              reader.skipValue()
              decodeKeys(reader, decodedFields)
            case Some(field) =>
              decodeKeys(
                reader,
                for {
                  m       <- decodedFields
                  decoded <- field.decoder.decode(reader)
                } yield m + (name -> decoded)
              )
          }
        }

      reader.beginObject()
      val hlist = decodeKeys(reader, Right(Map.empty))
      while (reader.peek() == JsonToken.NAME) {
        reader.nextName()
        reader.skipValue()
      }
      reader.endObject()

      hlist.flatMap(mapper.fromMap(_)).map(lg.from(_))
    }
}
