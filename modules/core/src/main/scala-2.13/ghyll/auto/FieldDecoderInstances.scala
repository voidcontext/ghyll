package ghyll.auto

import ghyll.Decoder
import ghyll.auto.FieldDecoder.Field
import shapeless._
import shapeless.labelled.FieldType

private[ghyll] trait FieldDecoderInstances {
  implicit def hnilFieldDecoder[F[_]]: FieldDecoder[F, HNil] =
    new FieldDecoder[F, HNil] {
      val fields: List[Field[F]] = List.empty
    }

  implicit def hconsFieldDecoder[F[_], K <: Symbol, V, T <: HList](implicit
    witness: Witness.Aux[K],
    d: Decoder[F, V],
    rest: FieldDecoder[F, T]
  ): FieldDecoder[F, FieldType[K, V] :: T] =
    new FieldDecoder[F, FieldType[K, V] :: T] {
      def fields: List[Field[F]] =
        new Field[F] {
          type Out = V
          val name = witness.value.name
          val decoder = d
        } :: rest.fields
    }

  implicit def genericFieldDecoder[F[_], T, G](implicit
    lg: LabelledGeneric.Aux[T, G],
    rest: FieldDecoder[F, G]
  ): FieldDecoder[F, T] = {
    val (_) = (lg) // To avoid unused variable warning / error
    new FieldDecoder[F, T] {
      val fields: List[Field[F]] = rest.fields
    }
  }
}
