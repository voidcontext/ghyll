package ghyll

import java.io.{File, FileInputStream, InputStream}

import cats.ApplicativeError
import cats.effect.{Resource, Sync}
import cats.instances.string._
import cats.syntax.eq._
import fs2.Pull._
import fs2.{Pull, Stream}
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName
import ghyll.jsonpath._

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
    fileInputStreamResource[F](json).flatMap(decodeObject(path, _))

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
    TokenStream.fromJson[F](json).evalMap { stream =>
      val _ = path
      stream.flatMap {
        case (JsonToken.BeginObject, _) =>
          d.decode(stream.tail).map(_.map(_._1))
        case _                          => Stream.emit(Left(StreamingDecodingFailure("Not an object!")))
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
    fileInputStreamResource[F](json)
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
    TokenStream
      .fromJson[F](json)
      .map(traversePath(path))
      .map(_.stream)
      .map { stream =>
        stream.pull.uncons1.flatMap {
          _.fold[Pull[F, DecoderResult[(String, T)], Unit]](
            Pull.output1(
              Left(
                StreamingDecodingFailure(s"Expected ${TokenName[JsonToken.BeginObject].show()} but got 'EndDocument'")
              )
            )
          ) {
            case ((JsonToken.BeginObject, _) -> tail) =>
              def decodeNext(stream: TokenStream[F]): Pull[F, DecoderResult[(String, T)], Unit] =
                stream.pull.uncons1.flatMap(
                  _.fold(
                    Pull.output1[F, DecoderResult[(String, T)]](
                      Left(
                        StreamingDecodingFailure(s"Expected ${TokenName[JsonToken.Key].show()} but got 'EndDocument'")
                      )
                    )
                  ) {
                    case ((JsonToken.EndObject, _) -> _)    => Pull.done
                    case ((JsonToken.Key(name), _) -> tail) =>
                      d.decode(tail)
                        .pull
                        .uncons1
                        .flatMap(
                          _.fold(
                            Pull.output1[F, DecoderResult[(String, T)]](
                              Left(StreamingDecodingFailure("This shouldn't happen"))
                            )
                          ) {
                            case (Right(v -> t) -> _) => Pull.output1(Right(name -> v)) >> decodeNext(t)
                            case (Left(err) -> _)     => Pull.output1(Left(err))
                          }
                        )
                    case ((token, pos) -> _)                =>
                      Pull.output1(
                        Left(
                          StreamingDecodingFailure(
                            s"Expected ${TokenName[JsonToken.Key].show()} or ${TokenName[JsonToken.EndObject]
                              .show()} but got ${TokenName(token).show()} at $pos "
                          )
                        )
                      )
                  }
                )

              decodeNext(tail)
            case ((token, pos) -> _)                  =>
              Pull.output1(
                Left(
                  StreamingDecodingFailure(
                    s"Expected ${TokenName[JsonToken.BeginObject].show()} but got ${TokenName(token).show()} at $pos "
                  )
                )
              )
          }
        }.stream
      }

  private def traversePath[F[_]](
    path: JsonPath
  )(stream: TokenStream[F])(implicit ae: ApplicativeError[F, Throwable]): Pull[F, (JsonToken, List[Pos]), Unit] = {
    path match {
      case JNil            => stream.pull.echo
      case phead >:: ptail =>
        stream.pull.uncons1.flatMap {
          case Some((JsonToken.BeginObject, _) -> tail) => traversePath(ptail)(skipUntil(phead, tail).stream)
          case Some((token, pos) -> _)                  =>
            Pull.raiseError(
              new IllegalStateException(
                s"Expected ${TokenName[JsonToken.BeginObject].show()} but got ${TokenName(token).show()} at $pos "
              )
            )
          case _                                        => Pull.raiseError(new IllegalStateException("This shouldn't happen"))
        }
    }
  }

  private def skipUntil[F[_]](name: String, stream: TokenStream[F])(implicit
    ae: ApplicativeError[F, Throwable]
  ): Pull[F, (JsonToken, List[Pos]), Unit] =
    stream.pull.uncons1.flatMap {
      _.fold[Pull[F, (JsonToken, List[Pos]), Unit]](
        Pull.raiseError(new IllegalStateException("Couldn't find given path"))
      ) {
        case ((JsonToken.Key(n), _)) -> tail if n === name => tail.pull.echo
        case (JsonToken.Key(_), _) -> tail                 => skipUntil(name, TokenStream.skipValue(tail))
        case _                                             => Pull.raiseError(new IllegalStateException("This shouldn't happen"))
      }
    }

  private def fileInputStreamResource[F[_]: Sync](file: File): Resource[F, InputStream] =
    Resource.fromAutoCloseable(Sync[F].delay(new FileInputStream(file)))

}
