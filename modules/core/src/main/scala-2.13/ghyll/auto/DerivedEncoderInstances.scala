package ghyll.auto

// import fs2.Stream
import ghyll.json.JsonToken._
// import ghyll.{Encoder, StreamingEncoderResult}
import ghyll.{StreamingEncoderResult}
import shapeless._
import shapeless.labelled.FieldType
import ghyll.Encoder

private[ghyll] trait DerivedEncoderInstances {
  implicit def derivedEncoderHNil: DerivedEncoder[HNil] =
    new DerivedEncoder[HNil] {
      def encode(value: HNil): StreamingEncoderResult =
        Right(LazyList.empty)
    }

  implicit def derivedEncoderHCons[K <: Symbol, H, T <: HList] (implicit
    witness: Witness.Aux[K],
    headEncoder: Encoder[H],
    tailEncoder: DerivedEncoder[T]
  ): DerivedEncoder[FieldType[K, H] :: T] =
    new DerivedEncoder[FieldType[K, H] :: T] {
      def encode(value: FieldType[K, H] :: T): StreamingEncoderResult =
        for {
          headStream <- headEncoder.encode(value.head)
          tailStream <- tailEncoder.encode(value.tail)
        } yield Key(witness.value.name) #:: (headStream ++ tailStream)
    }

  implicit def derivedEncoderGeneric[A, H] (implicit
    lg: LabelledGeneric.Aux[A, H],
    hconsEncoder: DerivedEncoder[H]
  ) : DerivedEncoder[A] =
    new DerivedEncoder[A] {
      def encode(value: A): StreamingEncoderResult =
        hconsEncoder.encode(lg.to(value)).map { BeginObject #:: _ ++ LazyList(EndObject) }
    }
}
