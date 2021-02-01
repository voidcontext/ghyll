package ghyll

import java.io.{InputStream, InputStreamReader}

import scala.annotation.tailrec

import cats.effect.{Resource, Sync}
import cats.instances.string._
import cats.syntax.eq._
import com.google.gson.stream.{JsonReader, JsonToken}
import fs2.Stream
import ghyll.gson.Implicits._
import ghyll.jsonpath._

private[ghyll] trait Decode {
  def decodeArray[F[_]: Sync, T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[T]]] = ???

  def decodeObject[F[_]: Sync, T: Decoder](json: InputStream): Resource[F, StreamingDecoderResult[T]] =
    decodeObject(JNil, json)

  def decodeObject[F[_]: Sync, T: Decoder](path: JsonPath, json: InputStream): Resource[F, StreamingDecoderResult[T]] =
    readerResource(json).map { reader =>
      if (reader.peek() === JsonToken.BEGIN_OBJECT) {
        traversePath(path, reader)
        Decoder[T].decode(reader)
      } else Left(StreamingDecodingFailure("Not an object!"))
    }

  def decodeKeyValues[F[_]: Sync, T](
    json: InputStream
  )(implicit d: Decoder[T]): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
    decodeKeyValues(JNil, json)

  def decodeKeyValues[F[_]: Sync, T](
    path: JsonPath,
    json: InputStream
  )(implicit d: Decoder[T]): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
    readerResource(json).map { reader =>
      Stream
        .eval(Sync[F].delay {
          traversePath(path, reader)
          reader.beginObject()
          reader
        })
        .flatMap(
          Stream.unfold(_) { reader =>
            if (reader.peek() === JsonToken.END_OBJECT) None
            else {
              val key = reader.nextName()
              Option(d.decode(reader).map(key -> _) -> reader)
            }
          }
        )
    }

  @tailrec
  private def traversePath(path: JsonPath, reader: JsonReader): JsonReader = {
    path match {
      case JNil          => reader
      case head >:: tail =>
        reader.beginObject()
        traversePath(tail, skipUntil(head, reader))
    }
  }

  @tailrec
  private def skipUntil(name: String, reader: JsonReader): JsonReader = {
    val current = reader.nextName()
    if (current === name) reader
    else {
      reader.skipValue()
      skipUntil(name, reader)
    }
  }

  private def readerResource[F[_]: Sync](json: InputStream): Resource[F, JsonReader] =
    Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(json))))

}
