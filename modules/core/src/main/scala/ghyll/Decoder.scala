package ghyll

import java.time.LocalDate

import scala.reflect.ClassTag

import cats.syntax.either._
import fs2.Pull._
import fs2.{Pull, Stream}
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName

trait Decoder[F[_], A] {
  def decode(stream: TokenStream[F]): StreamingDecoderResult[F, A]
}

object Decoder {
  def apply[F[_], A](implicit ev: Decoder[F, A]) = ev

  implicit def stringDecoder[F[_]]: Decoder[F, String] =
    createDecoder[F, String, JsonToken.Str] { case JsonToken.Str(v) => Right(v) }

  implicit def intDecoder[F[_]]: Decoder[F, Int] =
    createDecoder[F, Int, JsonToken.Number[String]] { case JsonToken.Number(v: String) =>
      Right(v.toInt)
    }

  implicit def booleanDecoder[F[_]]: Decoder[F, Boolean] =
    createDecoder[F, Boolean, JsonToken.Boolean] { case JsonToken.Boolean(v) => Right(v) }

  implicit def bigDecimalDecoder[F[_]]: Decoder[F, BigDecimal] =
    createDecoder[F, BigDecimal, JsonToken.Number[String]] { case JsonToken.Number(v: String) =>
      Right(BigDecimal(v))
    }

  implicit def localDateDecoder[F[_]]: Decoder[F, LocalDate] =
    createDecoder[F, LocalDate, JsonToken.Str] { case JsonToken.Str(v) => catchDecodingFailure(LocalDate.parse(v)) }

  implicit def optionDecoder[F[_], A](implicit aDecoder: Decoder[F, A]): Decoder[F, Option[A]] =
    _.pull.uncons1
      .flatMap[F, Either[StreamingDecoderError, (Option[A], TokenStream[F])], Unit] {
        case Some((JsonToken.Null, _) -> tail) => Pull.output1(Right(Option.empty[A] -> tail))
        case Some(tokenWithPos -> tail)        =>
          aDecoder.decode(tail.cons1(tokenWithPos)).map(_.map { case (a, s) => Some(a) -> s }).pull.echo
        case None                              => Pull.output1(Left(StreamingDecodingFailure(s"Expected optional value but got 'EndOfDocument'")))
      }
      .stream

  implicit def listDecoder[F[_], A](implicit aDecoder: Decoder[F, A]): Decoder[F, List[A]] =
    stream =>
      withExpected[F, List[A], JsonToken.BeginArray](stream) { case ((_: JsonToken.BeginArray, _), tail) =>
        def decodeNext(
          stream: TokenStream[F],
          result: List[A]
        ): Pull[F, Either[StreamingDecoderError, (List[A], TokenStream[F])], Unit] =
          stream.pull.uncons1.flatMap {
            case Some((JsonToken.EndArray, _) -> tail) => Pull.output1(Right(result.reverse -> tail))
            case Some(tokenWithPos -> tail)            =>
              aDecoder.decode(tail.cons1(tokenWithPos)).pull.uncons1.flatMap {
                case Some(Right(value -> tail) -> _) => decodeNext(tail, value :: result)
                case Some(Left(err) -> _)            => Pull.output1(Left(err))
                case None                            => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
              }
            case None                                  => Pull.output1(Left(StreamingDecodingFailure("Expected list item, but got 'EndOfDocument'")))
          }

        decodeNext(tail, List.empty).stream
      }

  implicit def mapDecoder[F[_], V](implicit valueDecoder: Decoder[F, V]): Decoder[F, Map[String, V]] =
    stream =>
      withExpected[F, Map[String, V], JsonToken.BeginObject](stream) { case ((_: JsonToken.BeginObject, _), tail) =>
        def decodeNext(
          stream: TokenStream[F],
          result: Map[String, V]
        ): Pull[F, Either[StreamingDecoderError, (Map[String, V], TokenStream[F])], Unit] =
          stream.pull.uncons1.flatMap {
            case Some((JsonToken.EndObject, _) -> tail) => Pull.output1((Right(result -> tail)))
            case Some((JsonToken.Key(name), _) -> tail) =>
              valueDecoder.decode(tail).pull.uncons1.flatMap {
                case Some(Right(value -> tail) -> _) => decodeNext(tail, result + (name -> value))
                case Some(Left(err) -> _)            => Pull.output1(Left(err))
                case None                            => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
              }
            case Some((token, pos) -> _)                =>
              Pull.output1(
                Left(
                  StreamingDecodingFailure(
                    s"Expected ${TokenName[JsonToken.Key].show()} but got ${TokenName(token).show()} at $pos"
                  )
                )
              )
            case None                                   =>
              Pull.output1(
                Left(StreamingDecodingFailure(s"Expected ${TokenName[JsonToken.Key].show()}, but got 'EndOfDocument'"))
              )
          }

        decodeNext(tail, Map.empty).stream
      }

  private def createDecoder[F[_], A, Token <: JsonToken: ClassTag: TokenName](
    pf: Token => Either[StreamingDecoderError, A]
  ): Decoder[F, A] =
    withExpected[F, A, Token](_) { case ((token: Token, _), stream) =>
      Stream.emit(pf(token).map(_ -> stream)).covary[F]
    }

  private[ghyll] def withExpected[F[_], A, Token <: JsonToken: TokenName](
    stream: TokenStream[F]
  )(
    pf: PartialFunction[((JsonToken, List[Pos]), TokenStream[F]), StreamingDecoderResult[F, A]]
  ): StreamingDecoderResult[F, A] =
    stream.pull.uncons1
      .flatMap[F, Either[StreamingDecoderError, (A, TokenStream[F])], Unit] {
        case Some(tokenWithPos -> tail) =>
          pf.orElse(expected[F, A, Token])(tokenWithPos -> tail).pull.echo
        case None                       =>
          Pull.output1(Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()} but got 'EndOfDocument'")))
      }
      .stream

  private def expected[F[_], A, Token <: JsonToken: TokenName]
    : PartialFunction[((JsonToken, List[Pos]), TokenStream[F]), StreamingDecoderResult[F, A]] = { case ((t, pos), _) =>
    Stream.emit(
      Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got ${TokenName(t).show()} at $pos"))
    )
  }

  // TODO: make sure pos is accessible here
  private def catchDecodingFailure[A](body: => A): Either[StreamingDecoderError, A] =
    Either.catchNonFatal(body).left.map[StreamingDecoderError](t => StreamingDecodingFailure(t.getMessage()))

}
