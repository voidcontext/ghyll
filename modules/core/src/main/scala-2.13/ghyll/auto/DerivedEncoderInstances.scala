package ghyll.auto

import fs2.Stream
import ghyll.json.JsonToken._
import ghyll.{Encoder, StreamingEncoderResult}
import shapeless._
import shapeless.labelled.FieldType

private[ghyll] trait DerivedEncoderInstances {
  implicit def derivedEncoderHNil[F[_]]: DerivedEncoder[F, HNil] =
    new DerivedEncoder[F, HNil] {
      def encode(value: HNil): StreamingEncoderResult[F] = Right(Stream.empty)
    }

  implicit def derivedEncoderHCons[F[_], K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    headEncoder: Encoder[F, H],
    tailEncoder: DerivedEncoder[F, T]
  ): DerivedEncoder[F, FieldType[K, H] :: T] =
    new DerivedEncoder[F, FieldType[K, H] :: T] {

      def encode(value: FieldType[K, H] :: T): StreamingEncoderResult[F] =
        for {
          headStream <- headEncoder.encode(value.head)
          tailStream <- tailEncoder.encode(value.tail)
        } yield Stream.emit(Key(witness.value.name)) ++ headStream ++ tailStream
    }

  implicit def derivedEncoderGeneric[F[_], A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    hconsEncoder: DerivedEncoder[F, H]
  ): DerivedEncoder[F, A] =
    new DerivedEncoder[F, A] {
      def encode(value: A): StreamingEncoderResult[F] =
        hconsEncoder.encode(lg.to(value)).map { Stream.emit(BeginObject) ++ _ ++ Stream.emit(EndObject) }
    }
}
