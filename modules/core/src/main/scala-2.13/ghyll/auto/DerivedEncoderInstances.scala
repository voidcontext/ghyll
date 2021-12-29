package ghyll.auto

import ghyll.Encoder
import shapeless._
import shapeless.labelled.FieldType

private[ghyll] trait DerivedEncoderInstances {
  implicit def derivedEncoderHNil[F[_]]: DerivedEncoder[F, HNil] = ???

  implicit def derivedEncoderHCons[F[_], K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    headEncoder: Encoder[F, H],
    tailEncoder: DerivedEncoder[F, T]
  ): DerivedEncoder[F, FieldType[K, H] :: T] = ???

  implicit def derivedEncoderGeneric[F[_], A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    hconsEncoder: DerivedEncoder[F, H]
  ): DerivedEncoder[F, A] = ???
}
