package ghyll.auto

import cats.data.EitherT
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{Applicative, Monad}
import ghyll.json.{JsonToken, JsonTokenWriter}
import ghyll.utils.EitherOps
import ghyll.{Encoder, StreamingEncoderError, StreamingEncoderResult}
import shapeless._
import shapeless.labelled.FieldType

private[ghyll] trait DerivedEncoderInstances {
  implicit def derivedEncoderHNil[F[_]: Applicative]: DerivedEncoder[F, HNil] =
    new DerivedEncoder[F, HNil] {
      def encode(value: HNil, writer: JsonTokenWriter[F]): StreamingEncoderResult[F] =
        Applicative[F].pure(Right(()))
    }

  implicit def derivedEncoderHCons[F[_]: Monad, K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    headEncoder: Encoder[F, H],
    tailEncoder: DerivedEncoder[F, T]
  ): DerivedEncoder[F, FieldType[K, H] :: T] =
    new DerivedEncoder[F, FieldType[K, H] :: T] {

      def encode(value: FieldType[K, H] :: T, writer: JsonTokenWriter[F]): StreamingEncoderResult[F] =
        writer.write(JsonToken.Key(witness.value.name)) >> (EitherT(headEncoder.encode(value.head, writer)) >> EitherT(
          tailEncoder.encode(value.tail, writer)
        )).value
    }

  implicit def derivedEncoderGeneric[F[_]: Monad, A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    hconsEncoder: DerivedEncoder[F, H]
  ): DerivedEncoder[F, A] =
    new DerivedEncoder[F, A] {

      def encode(value: A, writer: JsonTokenWriter[F]): StreamingEncoderResult[F] =
        writer.write(JsonToken.BeginObject) >> hconsEncoder.encode(lg.to(value), writer).flatMap {
          _.fold(
            err => Applicative[F].pure(Left(err)),
            _ => writer.write(JsonToken.EndObject).map(_.right[StreamingEncoderError])
          )
        }

    }
}
