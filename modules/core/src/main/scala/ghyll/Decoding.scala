package ghyll

import java.io.{File, FileInputStream, InputStream, InputStreamReader}

import cats.MonadError
import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.gson.stream.JsonReader
import fs2.Stream
import ghyll.json.JsonToken.TokenName
import ghyll.json.{JsonToken, JsonTokenReader}
import ghyll.jsonpath._
import ghyll.utils.EitherOps

private[ghyll] trait Decoding {
  def decodeArray[F[_]: Sync, T](json: InputStream)(implicit
    d: Decoder[F, T]
  ): Resource[F, Stream[F, DecoderResult[T]]] = ???

  /**
   * Shortcut of [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which safely
   * wraps the given file in a `Resource` and transforms it to an InputStream. It is using `JNil` as path.
   *
   * @param json
   *   the file containing the JSON
   * @return
   */
  def decodeObject[F[_]: Sync, T](json: File)(implicit d: Decoder[F, T]): Resource[F, DecoderResult[T]] =
    decodeObject(JNil, json)

  /**
   * Shortcut of [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which safely
   * wraps the given file in a `Resource` and transforms it to an InputStream.
   *
   * @param path
   *   the path needs to be traversed to find the required value
   * @param json
   *   the file containing the JSON
   * @return
   */
  def decodeObject[F[_]: Sync, T](path: JsonPath, json: File)(implicit
    d: Decoder[F, T]
  ): Resource[F, DecoderResult[T]] =
    fileInputStreamResource[F](json).flatMap(decodeObject(path, _))

  /**
   * Shortcut of [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which is using
   * `JNil` as path.
   *
   * @param json
   *   @return
   */
  def decodeObject[F[_]: Sync, T](json: InputStream)(implicit d: Decoder[F, T]): Resource[F, DecoderResult[T]] =
    decodeObject(JNil, json)

  /**
   * Parse the given JSON and decodes the value under the given JSON path
   *
   * @param path
   *   the path needs to be traversed to find the required value
   * @param json
   *   the JSON as input stream
   * @return
   */
  def decodeObject[F[_]: Sync, T](path: JsonPath, json: InputStream)(implicit
    d: Decoder[F, T]
  ): Resource[F, DecoderResult[T]] =
    readerResource(json).evalMap(traversePath(path)).evalMap(d.decode)

  /**
   * Shortcut of [[decodeKeyValues[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which safely
   * wraps the given file in a `Resource` and transforms it to an InputStream. It is using `JNil` as path
   *
   * @param json
   *   the file containing the JSON
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T](
    json: File
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
    decodeKeyValues(JNil, json)

  /**
   * Shortcut of [[decodeKeyValues[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which safely
   * wraps the given file in a `Resource`
   *
   * @param path
   *   a path of object attributes that needs to be traversed before the values can be emitted
   * @param json
   *   the file containing the JSON
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T](
    path: JsonPath,
    json: File
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
    fileInputStreamResource[F](json)
      .flatMap(decodeKeyValues(path, _))

  /**
   * Shortcut of [[decodeKeyValues[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which is using
   * `JNil` as path.
   *
   * @param json
   *   the JSON as input stream
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T](
    json: InputStream
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
    decodeKeyValues(JNil, json)

  /**
   * Safely parses the given JSON input stream and starts emitting key-value pairs once the given JSON path is
   * traversed.
   *
   * @tparam F
   *   effect type
   * @tparam T
   *   type of the value that will be encoded
   * @param path
   *   a path of object attributes that needs to be traversed before the values can be emitted
   * @param json
   *   the JSON as input stream
   * @param d
   *   the `Decoder` instance of T
   * @return
   */
  def decodeKeyValues[F[_]: Sync, T](
    path: JsonPath,
    json: InputStream
  )(implicit d: Decoder[F, T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
    readerResource(json).evalMap(traversePath(path)).map { reader =>
      Stream
        .eval(
          reader.next.map(
            _.fold[DecoderResult[JsonTokenReader[F]]](
              err => StreamingDecoderError.wrapError(err).left,
              {
                case (_, JsonToken.BeginObject) => reader.right
                case (p, t)                     => StreamingDecoderError.unexpectedToken(t, p).left
              }
            )
          )
        )
        .flatMap(
          _.fold(
            err => Stream.emit(err.left),
            reader =>
              Stream.unfoldEval(reader) { reader =>
                reader.next.flatMap[Option[(DecoderResult[(String, T)], JsonTokenReader[F])]] {
                  case Right((_, JsonToken.EndObject)) => Sync[F].pure(Option.empty)
                  case Right((_, JsonToken.Key(key)))  =>
                    d.decode(reader).map(_.map(value => (key -> value))).map(result => Some(result -> reader))
                  case Right((p, t))                   =>
                    Sync[F].raiseError(new RuntimeException(s"Expected ${TokenName[JsonToken.EndObject]
                      .show()} or ${TokenName[JsonToken.Key]}, but got ${TokenName(t).show()} at $p"))
                  case Left(err)                       => Sync[F].raiseError(new RuntimeException(s"Tokenizer error: $err"))
                }
              }
          )
        )
    }

  private def traversePath[F[_]](
    path: JsonPath
  )(reader: JsonTokenReader[F])(implicit F: MonadError[F, Throwable]): F[JsonTokenReader[F]] =
    path match {
      case JNil          => F.pure(reader)
      case head >:: tail => {
        reader.next.flatMap {
          case Right((_, JsonToken.BeginObject)) => skipUntil(head, reader).flatMap(traversePath(tail))
          // TODO: nicer error handling here
          case _                                 => F.raiseError(new RuntimeException("Error in traversePath"))
        }
      }
    }

  @SuppressWarnings(Array("scalafix:DisableSyntax.=="))
  private def skipUntil[F[_]](name: String, reader: JsonTokenReader[F])(implicit
    F: MonadError[F, Throwable]
  ): F[JsonTokenReader[F]] =
    reader.next.flatMap {
      case Right((_, JsonToken.Key(current))) if (current == name) => F.pure(reader)
      case Right((_, JsonToken.Key(_)))                            =>
        JsonTokenReader
          .skipValue(reader)
          .flatMap(result =>
            F.fromEither(result.left.map[Throwable](err => new RuntimeException(err.toString)).as(reader))
          )
          .flatMap(skipUntil(name, _))
      // TODO: nicer error handling here
      case _                                                       => F.raiseError(new RuntimeException("Error in skipUntil"))
    }

  private def readerResource[F[_]: Sync](json: InputStream): Resource[F, JsonTokenReader[F]] =
    Resource
      .fromAutoCloseable(
        Sync[F].delay(new JsonReader(new InputStreamReader(json)))
      )
      .evalMap(JsonTokenReader(_))

  private def fileInputStreamResource[F[_]: Sync](file: File): Resource[F, InputStream] =
    Resource.fromAutoCloseable(Sync[F].delay(new FileInputStream(file)))

}
