package ghyll.auto

import shapeless.LabelledGeneric

private[ghyll] trait DerivedDecoderInstances {
  implicit def derivedDecoderGeneric[F[_], A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[F, A],
    mapper: ReprMapper[H]
  ): DerivedDecoder[F, A] = ???

}
