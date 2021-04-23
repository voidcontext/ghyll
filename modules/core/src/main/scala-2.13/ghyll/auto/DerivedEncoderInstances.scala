package ghyll.auto

import cats.instances.either._
import cats.syntax.flatMap._
import com.google.gson.stream.JsonWriter
import ghyll.StreamingEncoderResult.catchEncodingFailure
import ghyll.{Encoder, StreamingEncoderResult}
import shapeless.labelled.FieldType
import shapeless.{LabelledGeneric, _}

private[ghyll] trait DerivedEncoderInstances {
  implicit def derivedEncoderHNil: DerivedEncoder[HNil] =
    new DerivedEncoder[HNil] {
      def encode(writer: JsonWriter, value: HNil): StreamingEncoderResult = Right(())
    }

  implicit def derivedEncoderHCons[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    headEncoder: Encoder[H],
    tailEncoder: DerivedEncoder[T]
  ): DerivedEncoder[FieldType[K, H] :: T] =
    new DerivedEncoder[FieldType[K, H] :: T] {
      def encode(writer: JsonWriter, value: FieldType[K, H] :: T): StreamingEncoderResult =
        catchEncodingFailure(writer.name(witness.value.name)) >>
          headEncoder.encode(writer, value.head) >>
          tailEncoder.encode(writer, value.tail)
    }

  implicit def derivedEncoderGeneric[A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    hconsEncoder: DerivedEncoder[H]
  ): DerivedEncoder[A] =
    new DerivedEncoder[A] {
      def encode(writer: JsonWriter, value: A): StreamingEncoderResult =
        catchEncodingFailure(writer.beginObject()) >>
          hconsEncoder.encode(writer, lg.to(value)) >>
          catchEncodingFailure(writer.endObject())
    }
}
