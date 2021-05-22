package ghyll.auto

import cats.ApplicativeError
import cats.syntax.eq._
import fs2.Stream
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName
import ghyll.{Decoder, StreamingDecoderResult, StreamingDecodingFailure, TokenStream}
import shapeless._

private[ghyll] object DerivedDecoderInstances {
  implicit def derivedDecoderGeneric[F[_], A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[F, A],
    mapper: ReprMapper[H],
    ae: ApplicativeError[F, Throwable]
  ): DerivedDecoder[F, A] =
    stream =>
      Decoder.withExpected[F, A, JsonToken.BeginObject](stream) { case (_: JsonToken.BeginObject, tail) =>
        decodeKeys(Stream.emit(Right(Map.empty[String, Any] -> tail)), fieldDecoders).flatMap {
          case Right((m, s)) =>
            Stream.emit(mapper.fromMap(m).map(lg.from(_) -> s))
          case Left(err)     =>
            Stream.emit(Left(err))
        }
      }

  private[this] def decodeKeys[F[_], A](
    stream: StreamingDecoderResult[F, Map[String, Any]],
    fieldDecoders: FieldDecoder[F, A]
  )(implicit ae: ApplicativeError[F, Throwable]): StreamingDecoderResult[F, Map[String, Any]] =
    stream.flatMap {
      _ match {
        case Right((m, str)) =>
          str.head.flatMap {
            case JsonToken.EndObject => Stream.emit(Right(m -> str.tail))
            case JsonToken.Key(name) =>
              fieldDecoders.fields.find(_.name === name) match {
                case None        => decodeKeys(Stream.emit(Right(m -> TokenStream.skipValue(str.tail))), fieldDecoders)
                case Some(field) =>
                  decodeKeys(
                    field.decoder.decode(str.tail).map(_.map { case (a, s) => m + (name -> a) -> s }),
                    fieldDecoders
                  )
              }
            case t                   => Stream.emit(Left(StreamingDecodingFailure(s"Unexpected token: ${TokenName(t).show()}")))
          }
        case e @ Left(_)     => Stream.emit(e)
      }
    }

  @SuppressWarnings(Array("scalafix:DisableSyntax.while"))
  @inline private[this] def skipRemainingKeys[F[_]](
    stream: TokenStream[F]
  )(implicit ae: ApplicativeError[F, Throwable]): TokenStream[F] =
    stream.head.flatMap {
      case JsonToken.Key(_) => skipRemainingKeys(TokenStream.skipValue(stream.tail))
      case _                => stream.tail
    }
}
