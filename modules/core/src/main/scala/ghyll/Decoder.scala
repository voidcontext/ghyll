package ghyll

import java.time.LocalDate

import scala.reflect.ClassTag

import cats.syntax.either._
import fs2.Stream
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName

trait Decoder[F[_], A] {
  def decode(stream: TokenStream[F]): StreamingDecoderResult[F, A]
}

object Decoder extends DecoderInstances {
  def apply[F[_], A](implicit ev: Decoder[F, A]) = ev

  implicit def stringDecoder[F[_]]: Decoder[F, String] =
    createDecoder[F, String, JsonToken.Str] { case JsonToken.Str(v) => Right(v) }

  implicit def intDecoder[F[_]]: Decoder[F, Int] =
    createDecoder[F, Int, JsonToken.Number] { case JsonToken.Number(v) => Right(v.toInt) }

  implicit def booleanDecoder[F[_]]: Decoder[F, Boolean] =
    createDecoder[F, Boolean, JsonToken.Boolean] { case JsonToken.Boolean(v) => Right(v) }

  implicit def bigDecimalDecoder[F[_]]: Decoder[F, BigDecimal] =
    createDecoder[F, BigDecimal, JsonToken.Number] { case JsonToken.Number(v) => Right(BigDecimal(v)) }

  implicit def localDateDecoder[F[_]]: Decoder[F, LocalDate] =
    createDecoder[F, LocalDate, JsonToken.Str] { case JsonToken.Str(v) => catchDecodingFailure(LocalDate.parse(v)) }

  implicit def optionDecoder[F[_], A](implicit aDecoder: Decoder[F, A]): Decoder[F, Option[A]] =
    stream =>
      stream.head.flatMap {
        case JsonToken.Null => Stream.emit(Right(Option.empty[A] -> stream.tail))
        case _              => aDecoder.decode(stream).map(_.map { case (a, s) => Some(a) -> s })
      }

  implicit def listDecoder[F[_], A](implicit aDecoder: Decoder[F, A]): Decoder[F, List[A]] =
    stream =>
      withExpected[F, List[A], JsonToken.BeginArray](stream) { case (_: JsonToken.BeginArray, tail) =>
        def decodeNext(s: StreamingDecoderResult[F, List[A]]): StreamingDecoderResult[F, List[A]] =
          s.flatMap {
            _ match {
              case Right((xs, str)) =>
                str.head.flatMap {
                  case JsonToken.EndArray => Stream.emit(Right(xs.reverse -> str.tail))
                  case _                  => decodeNext(aDecoder.decode(str).map(_.map { case (a, s) => (a :: xs) -> s }))
                }
              case e @ Left(_)      => Stream.emit(e)
            }
          }
        decodeNext(Stream.emit(Right(List.empty[A] -> tail)))

      }

  implicit def mapDecoder[F[_], V](implicit valueDecoder: Decoder[F, V]): Decoder[F, Map[String, V]] =
    stream =>
      withExpected[F, Map[String, V], JsonToken.BeginObject](stream) { case (_: JsonToken.BeginObject, tail) =>
        def decodeNext(s: StreamingDecoderResult[F, Map[String, V]]): StreamingDecoderResult[F, Map[String, V]] =
          s.flatMap {
            _ match {
              case Right((map, str)) =>
                str.head.flatMap {
                  case JsonToken.EndObject => Stream.emit(Right(map -> str.tail))
                  case JsonToken.Key(name) =>
                    decodeNext(valueDecoder.decode(str.tail).map(_.map { case (a, s) => map + (name -> a) -> s }))
                  case t                   => Stream.emit(Left(StreamingDecodingFailure(s"Unexpected token: ${TokenName(t).show()}")))
                }
              case e @ Left(_)       => Stream.emit(e)
            }
          }
        decodeNext(Stream.emit(Right(Map.empty[String, V] -> tail)))
      }

  private def createDecoder[F[_], A, Token <: JsonToken: ClassTag](
    pf: Token => Either[StreamingDecoderError, A]
  )(implicit tn: TokenName[Token]): Decoder[F, A] =
    withExpected[F, A, Token](_) { case (token: Token, tail) =>
      Stream.emit(pf(token).map(_ -> tail)).covary[F]
    }

  private[ghyll] def withExpected[F[_], A, Token <: JsonToken: TokenName](
    stream: Stream[F, JsonToken]
  )(
    pf: PartialFunction[(JsonToken, Stream[F, JsonToken]), StreamingDecoderResult[F, A]]
  ): StreamingDecoderResult[F, A] =
    stream.head.flatMap(token => pf.orElse(expected[F, A, Token].compose(dropStream))(token -> stream.tail))

  private def dropStream[F[_], A, Token <: JsonToken]: PartialFunction[(JsonToken, Stream[F, JsonToken]), JsonToken] = {
    case (t, _) => t
  }

  private def expected[F[_], A, Token <: JsonToken: TokenName]
    : PartialFunction[JsonToken, StreamingDecoderResult[F, A]] = { case t =>
    Stream.emit(Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got ${TokenName(t).show()}")))
  }

  private def catchDecodingFailure[A](body: => A): Either[StreamingDecoderError, A] =
    Either.catchNonFatal(body).left.map[StreamingDecoderError](t => StreamingDecodingFailure(t.getMessage()))

}
