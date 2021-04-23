package ghyll

import java.io.{File, FileInputStream, InputStream, InputStreamReader}

import scala.annotation.tailrec

import cats.effect.{Resource, Sync}
import cats.instances.string._
import cats.syntax.eq._
import com.google.gson.stream.{JsonReader, JsonToken}
import fs2.Stream
import ghyll.gson.Implicits._
import ghyll.jsonpath._

private[ghyll] trait Decoding {
  def decodeArray[F[_]: Sync, T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[T]]] = ???

  /**
   * Shortcut of
   * [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which safely wraps the given file in a `Resource` and transforms
   * it to an InputStream. It is using `JNil` as path.
   *
   * @param json the file containing the JSON
   * @return
   */
  def decodeObject[F[_]: Sync, T: Decoder](json: File): Resource[F, StreamingDecoderResult[T]] =
    decodeObject(JNil, json)

  /**
   * Shortcut of
   * [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which safely wraps the given file in a `Resource` and transforms
   * it to an InputStream.
   *
   * @param path the path needs to be traversed to find the required
   *             value
   * @param json the file containing the JSON
   * @return
   */
  def decodeObject[F[_]: Sync, T: Decoder](path: JsonPath, json: File): Resource[F, StreamingDecoderResult[T]] =
    fileInputStreamResource(json).flatMap(decodeObject(path, _))

  /**
   * Shortcut of
   * [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which is using `JNil` as path.
   *
   * @param json
   * @return
   */
  def decodeObject[F[_]: Sync, T: Decoder](json: InputStream): Resource[F, StreamingDecoderResult[T]] =
    decodeObject(JNil, json)

  /**
   * Parse the given JSON and decodes the value under the given JSON
   * path
   *
   * @param path the path needs to be traversed to find the required
   *             value
   * @param json the JSON as input stream
   * @return
   */
  def decodeObject[F[_]: Sync, T: Decoder](path: JsonPath, json: InputStream): Resource[F, StreamingDecoderResult[T]] =
    readerResource(json).map { reader =>
      if (reader.peek() === JsonToken.BEGIN_OBJECT) {
        traversePath(path, reader)
        Decoder[T].decode(reader)
      } else Left(StreamingDecodingFailure("Not an object!"))
    }

  /**
   * Shortcut of
   * [[decodeKeyValues[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which safely wraps the given file in a `Resource` and transforms it to an InputStream. It is using `JNil` as path
   *
   * @param json the file containing the JSON
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T: Decoder](
    json: File
  ): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
    decodeKeyValues(JNil, json)

  /**
   * Shortcut of
   * [[decodeKeyValues[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which safely wraps the given file in a `Resource`
   *
   * @param path a path of object attributes that needs to be
   *             traversed before the values can be emitted
   * @param json the file containing the JSON
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T: Decoder](
    path: JsonPath,
    json: File
  ): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
    fileInputStreamResource(json)
      .flatMap(decodeKeyValues(path, _))

  /**
   * Shortcut of
   * [[decodeKeyValues[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which is using `JNil` as path.
   *
   * @param json the JSON as input stream
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T: Decoder](
    json: InputStream
  ): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
    decodeKeyValues(JNil, json)

  /**
   * Safely parses the given JSON input stream and starts emitting
   * key-value pairs once the given JSON path is traversed.
   *
   * @tparam F effect type
   * @tparam T type of the value that will be encoded
   * @param path a path of object attributes that needs to be
   *             traversed before the values can be emitted
   * @param json the JSON as input stream
   * @param d the `Decoder` instance of T
   * @return
   */
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

  private def fileInputStreamResource[F[_]: Sync](file: File): Resource[F, InputStream] =
    Resource.fromAutoCloseable(Sync[F].delay(new FileInputStream(file)))

}
