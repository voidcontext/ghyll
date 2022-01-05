package ghyll

import java.time.LocalDate

import cats.Functor
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import ghyll.StreamingDecoderError.notExpectedToken
import ghyll.StreamingDecoderResult.wrapError
import ghyll.json.JsonToken.TokenName
import ghyll.json.{JsonToken, JsonTokenReader}
import ghyll.utils.EitherOps

trait Decoder[F[_], A] {
  def decode(reader: JsonTokenReader[F]): F[DecoderResult[A]]
}

object Decoder {
  def apply[F[_], A](implicit ev: Decoder[F, A]) = ev

  implicit def stringDecoder[F[_]: Functor]: Decoder[F, String] =
    createDecoder[F, JsonToken.Str, String] { case JsonToken.Str(v) =>
      Right(v)
    }

  implicit def intDecoder[F[_]: Functor]: Decoder[F, Int] =
    createDecoder[F, JsonToken.Number[Int], Int] { case JsonToken.Number(s: Int) =>
      Right(s)
    }

  implicit def booleanDecoder[F[_]: Functor]: Decoder[F, Boolean] =
    createDecoder[F, JsonToken.Boolean, Boolean] { case JsonToken.Boolean(b) =>
      Right(b)
    }

  implicit def bigDecimalDecoder[F[_]: Functor]: Decoder[F, BigDecimal] =
    createDecoder[F, JsonToken.Number[BigDecimal], BigDecimal] { case JsonToken.Number(s: String) =>
      catchDecodingFailure(BigDecimal(s))
//      case JsonToken.Number(i: Int) => BigDecimel(i)
    }

  implicit def localDateDecoder[F[_]: Functor]: Decoder[F, LocalDate] =
    createDecoder[F, JsonToken.Str, LocalDate] { case JsonToken.Str(v) =>
      catchDecodingFailure(LocalDate.parse(v))
    }

  implicit def optionDecoder[F[_]: Sync, A](implicit aDecoder: Decoder[F, A]): Decoder[F, Option[A]] =
    reader =>
      reader.next.flatMap {
        case Right((_, JsonToken.Null)) => Option.empty.right[StreamingDecoderError].pure[F]
        case Right(tp)                  =>
          for {
            r      <- JsonTokenReader.prepend(tp, reader)
            result <- aDecoder.decode(r)
          } yield result.map(Option(_))
        case Left(err)                  => wrapError[F, Option[A]](err)
      }

  implicit def listDecoder[F[_]: Sync, A](implicit aDecoder: Decoder[F, A]): Decoder[F, List[A]] =
    reader => {
      def decodeNext(reader: JsonTokenReader[F], result: List[A]): StreamingDecoderResult[F, List[A]] =
        reader.next.flatMap {
          case Right((_, JsonToken.EndArray)) => result.right[StreamingDecoderError].pure[F]
          case Right(tp)                      =>
            prependAndDecode(tp, reader, aDecoder).map(_.map(_ :: result)).flatMap {
              case Right(r) => decodeNext(reader, r)
              case err      => err.pure[F]
            }
          case Left(err)                      => wrapError[F, List[A]](err)
        }

      reader.next.flatMap {
        case Right((_, JsonToken.BeginArray)) => decodeNext(reader, Nil)
        case Right(tp)                        => createDecodingFailure[JsonToken.BeginArray, List[A]].apply(tp).pure[F]
        case Left(err)                        => wrapError[F, List[A]](err)
      }.map(_.map(_.reverse))

    }

  implicit def mapDecoder[F[_]: Sync, V](implicit valueDecoder: Decoder[F, V]): Decoder[F, Map[String, V]] =
    reader => {
      def decodeNext(reader: JsonTokenReader[F], result: Map[String, V]): StreamingDecoderResult[F, Map[String, V]] =
        reader.next.flatMap {
          case Right((_, JsonToken.EndObject)) => result.right[StreamingDecoderError].pure[F]
          case Right((_, JsonToken.Key(n)))    =>
            valueDecoder.decode(reader).flatMap {
              case Right(r)  => decodeNext(reader, result + (n -> r))
              case Left(err) => err.left[Map[String, V]].pure[F]
            }
          case Right(tp)                       => createDecodingFailure[JsonToken.Key, Map[String, V]].apply(tp).pure[F]
          case Left(err)                       => wrapError[F, Map[String, V]](err)
        }

      reader.next.flatMap {
        case Right((_, JsonToken.BeginObject)) => decodeNext(reader, Map.empty)
        case Right(tp)                         => createDecodingFailure[JsonToken.BeginObject, Map[String, V]].apply(tp).pure[F]
        case Left(err)                         => wrapError[F, Map[String, V]](err)
      }

    }

  private def prependAndDecode[F[_]: Sync, A](
    token: (List[Pos], JsonToken),
    reader: JsonTokenReader[F],
    decoder: Decoder[F, A]
  ) =
    JsonTokenReader.prepend(token, reader).flatMap(decoder.decode)

  private def createDecoder[F[_]: Functor, T <: JsonToken: TokenName, A](
    extractor: PartialFunction[JsonToken, DecoderResult[A]]
  ): Decoder[F, A] =
    _.next.map(
      _.fold(
        err => Left(StreamingDecodingFailure(s"Got error: $err")),
        extractor.compose[(List[Pos], JsonToken)](_._2).orElse(createDecodingFailure).apply
      )
    )

  private def createDecodingFailure[T <: JsonToken: TokenName, A]
    : PartialFunction[(List[Pos], JsonToken), DecoderResult[A]] = { case (p, v) =>
    Left(notExpectedToken[T](v, p))
  }

  // TODO: make sure pos is accessible here
  private def catchDecodingFailure[A](body: => A): Either[StreamingDecoderError, A] =
    Either.catchNonFatal(body).left.map[StreamingDecoderError](t => StreamingDecodingFailure(t.getMessage()))

}
