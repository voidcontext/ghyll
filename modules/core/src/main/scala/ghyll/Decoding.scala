package ghyll

import java.io.{File, FileInputStream, InputStream}

import cats.effect.{Resource, Sync}
import cats.syntax.eq._
import fs2.Stream
import ghyll.json.JsonToken._
import ghyll.jsonpath._

private[ghyll] trait Decoding {
  def decodeArray[F[_]: Sync, T](json: InputStream)(implicit
    d: Decoder[T]
  ): Resource[F, Stream[F, DecoderResult[T]]] = ???

  /**
   * Shortcut of [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which safely
   * wraps the given file in a `Resource` and transforms it to an InputStream. It is using `JNil` as path.
   *
   * @param json
   *   the file containing the JSON
   * @return
   */
  def decodeObject[F[_]: Sync, T](json: File)(implicit d: Decoder[T]): Resource[F, DecoderResult[T]] =
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
    d: Decoder[T]
  ): Resource[F, DecoderResult[T]] =
    fileInputStreamResource[F](json).flatMap(decodeObject(path, _))

  /**
   * Shortcut of [[decodeObject[F[_],T](path:ghyll\.jsonpath\.JsonPath,json:java\.io\.InputStream)*]], which is using
   * `JNil` as path.
   *
   * @param json
   *   @return
   */
  def decodeObject[F[_]: Sync, T](json: InputStream)(implicit d: Decoder[T]): Resource[F, DecoderResult[T]] =
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
    d: Decoder[T]
  ): Resource[F, DecoderResult[T]] =
    TokenStream.fromJson[F](json).map { stream =>
      val _ = path

      stream match {
        case Right(BeginObject -> _) #:: tail =>
          d.decode(tail).map(_._1)
        case _                                => Left(StreamingDecodingFailure("Not an object!"))
      }
    }

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
  )(implicit d: Decoder[T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
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
  )(implicit d: Decoder[T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
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
  )(implicit d: Decoder[T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
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
  )(implicit d: Decoder[T]): Resource[F, Stream[F, DecoderResult[(String, T)]]] =
    TokenStream
      .fromJson[F](json)
      .map(traversePath(path))
      .map {
        case Right(BeginObject -> _) #:: tail =>
          def decodeNext(s: TokenStream): Option[(DecoderResult[(String, T)], TokenStream)] =
            s.headOption.map {
              _.fold(
                _ => Some(Left(StreamingDecodingFailure("Tokenizer error.")) -> LazyList.empty),
                {
                  case EndObject -> _ => None
                  case Key(key) -> _  =>
                    Some(
                      d.decode(s.tail)
                        .fold[(DecoderResult[(String, T)], TokenStream)](
                          err => Left(err) -> s.tail,
                          { case (value, remaining) => Right(key -> value) -> remaining }
                        )
                    )
                  case _              =>
                    Some(Left(StreamingDecodingFailure("Unexpected token")) -> LazyList.empty)
                }
              )
            }.getOrElse(Some(Left(StreamingDecodingFailure("Unexpected end of token stream")) -> LazyList.empty))

          Stream.unfold(tail)(decodeNext)
        case _                                =>
          Stream.emit(Left(StreamingDecodingFailure("Not an object")))
      }

  private def traversePath(
    path: JsonPath
  )(stream: TokenStream): TokenStream =
    path match {
      case JNil            => stream
      case phead >:: ptail =>
        stream.headOption.map {
          _.fold(
            error => LazyList(Left(error)),
            {
              case BeginObject -> _ => traversePath(ptail)(skipUntil(phead, stream.tail))
              case token -> pos     =>
                LazyList(
                  Left(
                    NestingError(
                      s"Expected ${TokenName[BeginObject].show()} but got ${TokenName(token).show()} at $pos "
                    )
                  )
                )

            }
          )
        }.getOrElse(LazyList(Left(JsonPathError)))
    }

  private def skipUntil(name: String, stream: TokenStream): TokenStream =
    stream.headOption
      .map(
        _.fold(
          _ => LazyList(Left(JsonPathError)),
          {
            case Key(n) -> _ if n === name => stream.tail
            case Key(_) -> _               => skipUntil(name, TokenStream.skipValue(stream.tail))
            case _                         => LazyList(Left(JsonPathError))
          }
        )
      )
      .getOrElse(LazyList(Left(JsonPathError)))

  private def fileInputStreamResource[F[_]: Sync](file: File): Resource[F, InputStream] =
    Resource.fromAutoCloseable(Sync[F].delay(new FileInputStream(file)))

}
