package ghyll

import java.io.{InputStream, InputStreamReader}

import cats.effect.{Resource, Sync}
import cats.syntax.eq._
import com.google.gson.stream.{JsonReader, JsonToken}
import fs2.Stream
import ghyll.gson.Implicits._

private[ghyll] trait Decode {
  def decodeArray[F[_]: Sync, T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[T]]] = ???

  def decodeObject[F[_]: Sync, T: Decoder](json: InputStream): Resource[F, StreamingDecoderResult[T]] =
    readerResource(json).map { reader =>
      if (reader.peek() === JsonToken.BEGIN_OBJECT) {
        Decoder[T].decode(reader)
      } else Left(StreamingDecodingFailure("Not an object!"))
    }

  def decodeKeyValues[F[_]: Sync, T](
    json: InputStream
  )(implicit d: Decoder[T]): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
    readerResource(json).map { reader =>
      Stream
        .eval(Sync[F].delay { reader.beginObject(); reader })
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

  private def readerResource[F[_]: Sync](json: InputStream): Resource[F, JsonReader] =
    Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(json))))

}
