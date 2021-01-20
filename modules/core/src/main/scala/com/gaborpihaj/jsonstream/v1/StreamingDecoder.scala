package com.gaborpihaj.jsonstream.v1

import java.io.{File, FileInputStream, InputStreamReader}

import cats.effect.{Resource, Sync}
import cats.instances.either._
import cats.syntax.flatMap._
import com.google.gson.stream.{JsonReader, JsonToken}
import fs2.Stream
import io.circe.{Decoder, DecodingFailure, Json, JsonNumber, JsonObject}
import mouse.boolean._

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
object StreamingDecoder {
  sealed trait StreamingDecoderError
  case object Unimplemented extends StreamingDecoderError
  case class StreamingDecodingFailure(underlying: DecodingFailure) extends StreamingDecoderError

  type DecoderErrorOr[A] = Either[StreamingDecoderError, A]

  type Preprocessor = JsonReader => Unit
  object Preprocessor {
    val noop: Preprocessor = _ => ()

    def root(key: String): Preprocessor =
      reader =>
        if (reader.nextName() == key) reader.beginObject()
        else {
          reader.skipValue()
          root(key)(reader)
        }

    val dataOnly: Preprocessor = root("data")
  }

  def decode[F[_]: Sync, T: Decoder](
    file: File,
    preprocessor: Preprocessor = Preprocessor.noop,
    streamValues: Boolean = true
  ): Resource[F, Stream[F, Either[StreamingDecoderError, (String, T)]]] =
    readerResource(file).map { reader =>
      Stream
        .eval(
          Sync[F].delay {
            reader.beginObject()
            preprocessor(reader)
            reader
          }
        )
        .flatMap(Stream.unfold(_)(process[T](_, streamValues)))
    }

  private def process[T: Decoder](
    reader: JsonReader,
    streamValues: Boolean
  ): Option[(DecoderErrorOr[(String, T)], JsonReader)] =
    if (reader.peek() == JsonToken.END_OBJECT) None
    else Option(decodeNext(reader, streamValues) -> reader)

  private def decodeNext[T: Decoder](reader: JsonReader, streamValues: Boolean): DecoderErrorOr[(String, T)] = {
    val key = (streamValues).fold(reader.nextName(), "") // key of the root object

    parseObject(reader, streamValues) >>= { json =>
      Decoder[T].decodeJson(json).left.map(StreamingDecodingFailure(_)).map(key -> _)
    }
  }

  private def parse(reader: JsonReader): DecoderErrorOr[Json] =
    reader.peek() match {
      case JsonToken.BEGIN_OBJECT => parseObject(reader, true)
      case JsonToken.BEGIN_ARRAY  => parseArray(reader)
      case JsonToken.NUMBER       =>
        Right(
          Json.fromJsonNumber(
            JsonNumber.fromDecimalStringUnsafe(reader.nextString())
          )
        )
      case JsonToken.STRING       => Right(Json.fromString(reader.nextString()))
      case JsonToken.BOOLEAN      => Right(Json.fromBoolean(reader.nextBoolean()))
      case JsonToken.NULL         => reader.nextNull(); Right(Json.Null)
      case _                      => Left(Unimplemented)
    }

  private def parseArray(reader: JsonReader): DecoderErrorOr[Json] = {
    reader.beginArray()
    parseArrayValues(reader, Vector.empty).map(Json.arr).flatTap(_ => Right(reader.endArray()))
  }

  private def parseArrayValues(reader: JsonReader, array: Vector[Json]): DecoderErrorOr[Vector[Json]] =
    if (reader.peek() == JsonToken.END_ARRAY) Right(array)
    else parse(reader) >>= { v => parseArrayValues(reader, array :+ v) }

  private def parseObject(reader: JsonReader, streamValues: Boolean): DecoderErrorOr[Json] = {
    if (streamValues) reader.beginObject()
    parseKeys(reader, JsonObject()).map(Json.fromJsonObject).flatTap(_ => Right(reader.endObject()))
  }

  private def parseKeys(reader: JsonReader, obj: JsonObject): DecoderErrorOr[JsonObject] =
    if (reader.peek() == JsonToken.END_OBJECT) Right(obj)
    else
      for {
        key    <- Right[StreamingDecoderError, String](reader.nextName())
        value  <- parse(reader)
        result <- parseKeys(reader, obj.add(key, value))
      } yield result

  private def readerResource[F[_]: Sync](file: File): Resource[F, JsonReader] =
    for {
      ins    <- Resource.fromAutoCloseable(Sync[F].delay(new FileInputStream(file)))
      reader <- Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(ins))))
    } yield reader

}
