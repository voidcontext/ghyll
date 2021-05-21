package ghyll

import java.io.{File, FileInputStream, InputStream, InputStreamReader}

import scala.annotation.tailrec

import cats.effect.{Resource, Sync}
import cats.instances.string._
import cats.syntax.eq._
import com.google.gson.stream.{JsonReader, JsonToken => GsonToken}
import fs2.Stream
import ghyll.json.JsonToken
import ghyll.jsonpath._

@SuppressWarnings(Array("scalafix:DisableSyntax.throw")) // For now....
private[ghyll] trait Decoding {
  def decodeArray[F[_]: Sync, T](json: InputStream)(implicit
    d: Decoder[F, T]
  ): Resource[F, Stream[F, DecoderResult[T]]] = ???

  /**
   * Shortcut of
   * [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which safely wraps the given file in a `Resource` and transforms
   * it to an InputStream. It is using `JNil` as path.
   *
   * @param json the file containing the JSON
   * @return
   */
  def decodeObject[F[_]: Sync, T](json: File)(implicit d: Decoder[F, T]): Resource[F, DecoderResult[T]] =
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
  def decodeObject[F[_]: Sync, T](path: JsonPath, json: File)(implicit
    d: Decoder[F, T]
  ): Resource[F, DecoderResult[T]] =
    fileInputStreamResource(json).flatMap(decodeObject(path, _))

  /**
   * Shortcut of
   * [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which is using `JNil` as path.
   *
   * @param json
   * @return
   */
  def decodeObject[F[_]: Sync, T](json: InputStream)(implicit d: Decoder[F, T]): Resource[F, DecoderResult[T]] =
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
  def decodeObject[F[_]: Sync, T](path: JsonPath, json: InputStream)(implicit
    d: Decoder[F, T]
  ): Resource[F, DecoderResult[T]] =
    tokenStream(json).evalMap { stream =>
      val _ = path
      stream.flatMap {
        case JsonToken.BeginObject =>
          d.decode(stream.tail).map(_.map(_._1))
        case _                     => Stream.emit(Left(StreamingDecodingFailure("Not an object!")))
      }.compile.lastOrError
    }

  /**
   * Shortcut of
   * [[decodeKeyValues[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]],
   * which safely wraps the given file in a `Resource` and transforms it to an InputStream. It is using `JNil` as path
   *
   * @param json the file containing the JSON
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T](
    json: File
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
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
  def decodeKeyValues[F[_]: Sync, T](
    path: JsonPath,
    json: File
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
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
  def decodeKeyValues[F[_]: Sync, T](
    json: InputStream
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
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
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
    tokenStream(json).map { stream =>
      val _ = path
      stream.flatMap {
        case JsonToken.BeginObject =>
          def decodeNext(s: Stream[F, JsonToken]): Stream[F, DecoderResult[(String, T)]] =
            s.flatMap {
              case JsonToken.EndObject => Stream.empty
              case JsonToken.Key(k)    =>
                d.decode(s.tail).flatMap {
                  case Right((v, t)) => Stream.emit(Right(k -> v)) ++ decodeNext(t)
                  case Left(err)     => Stream.emit(Left(err))
                }
              case _                   => Stream.emit(Left(StreamingDecodingFailure("")))
            }
          decodeNext(stream.tail)
        case _                     => Stream.emit(Left(StreamingDecodingFailure("Not an object!")))

      }
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

  private def tokenStream[F[_]: Sync](json: InputStream): Resource[F, Stream[F, JsonToken]] =
    readerResource(json).map { reader =>
      Stream.unfold(nextToken(reader)) { token =>
        Option.when(reader.hasNext())(token -> nextToken(reader))
      }
    }

  private def nextToken(reader: JsonReader): JsonToken =
    reader.peek() match {
      case GsonToken.BEGIN_ARRAY  => reader.beginArray(); JsonToken.BeginArray
      case GsonToken.END_ARRAY    => reader.endArray(); JsonToken.EndArray
      case GsonToken.BEGIN_OBJECT => reader.beginObject(); JsonToken.BeginObject
      case GsonToken.END_OBJECT   => reader.endObject(); JsonToken.EndArray
      case GsonToken.NUMBER       => JsonToken.Number(reader.nextString())
      case GsonToken.STRING       => JsonToken.Str(reader.nextString())
      case GsonToken.NAME         => JsonToken.Key(reader.nextName())
      case GsonToken.BOOLEAN      => JsonToken.Boolean(reader.nextBoolean())
      case _                      => throw new RuntimeException("Unimplemented")
    }

  private def readerResource[F[_]: Sync](json: InputStream): Resource[F, JsonReader] =
    Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(json))))

  private def fileInputStreamResource[F[_]: Sync](file: File): Resource[F, InputStream] =
    Resource.fromAutoCloseable(Sync[F].delay(new FileInputStream(file)))

}
