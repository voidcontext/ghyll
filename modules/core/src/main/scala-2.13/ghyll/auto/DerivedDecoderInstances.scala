package ghyll.auto

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import ghyll.DecoderResult
import ghyll.StreamingDecoderError._
import ghyll.StreamingDecoderResult.wrapError
import ghyll.json.JsonTokenReader.skipValue
import ghyll.json.{JsonToken, JsonTokenReader}
import ghyll.utils.EitherOps
import shapeless.LabelledGeneric

private[ghyll] trait DerivedDecoderInstances {
  implicit def derivedDecoderGeneric[F[_]: Monad, A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[F, A],
    mapper: ReprMapper[H]
  ): DerivedDecoder[F, A] =
    reader =>
      reader.next.flatMap {
        case Right((_, JsonToken.BeginObject)) =>
          decodeKeys(reader, Map.empty, fieldDecoders).map(_.flatMap(repr => mapper.fromMap(repr).map(lg.from(_))))
        case Right((p, t))                     => notExpectedToken[JsonToken.BeginObject](t, p).left.pure[F]
        case Left(err)                         => wrapError(err)
      }

  @SuppressWarnings(Array("scalafix:DisableSyntax.=="))
  private def decodeKeys[F[_]: Monad, A](
    reader: JsonTokenReader[F],
    acc: Map[String, Any],
    fieldDecoders: FieldDecoder[F, A]
  ): F[DecoderResult[Map[String, Any]]] =
    reader.next.flatMap {
      case Right((_, JsonToken.EndObject)) => acc.right.pure[F]
      case Right((_, JsonToken.Key(n)))    =>
        fieldDecoders.fields
          .find(_.name == n)
          .fold(EitherT(skipValue(reader)).flatMapF(_ => decodeKeys(reader, acc, fieldDecoders))) { field =>
            EitherT(field.decoder.decode(reader)).flatMapF(value =>
              decodeKeys(reader, acc + (n -> value), fieldDecoders)
            )
          }
          .value
      case Right((p, t))                   => wrapError(unexpectedToken(t, p))
      case Left(err)                       => wrapError(err)
    }

}
