package ghyll.auto

// import fs2.Stream
import ghyll.json.JsonToken._
import ghyll.{Encoder, StreamingEncoderResult}
import shapeless._
import shapeless.labelled.FieldType

private[ghyll] trait DerivedEncoderInstances {
  implicit def derivedEncoderHNil: DerivedEncoder[HNil] =
    new DerivedEncoder[HNil] {
      def encode(value: HNil): StreamingEncoderResult =
        LazyList.empty
    }

  implicit def derivedEncoderHCons[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    headEncoder: Encoder[H],
    tailEncoder: DerivedEncoder[T]
  ): DerivedEncoder[FieldType[K, H] :: T] =
    new DerivedEncoder[FieldType[K, H] :: T] {
      def encode(value: FieldType[K, H] :: T): StreamingEncoderResult =
        Right(Key(witness.value.name)) #:: headEncoder.encode(value.head) ++ tailEncoder.encode(value.tail)
    }

  implicit def derivedEncoderGeneric[A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    hconsEncoder: DerivedEncoder[H]
  ): DerivedEncoder[A] =
    new DerivedEncoder[A] {
      def encode(value: A): StreamingEncoderResult =
        Right(BeginObject) #:: hconsEncoder.encode(lg.to(value)) ++ LazyList(Right(EndObject))
    }
}
