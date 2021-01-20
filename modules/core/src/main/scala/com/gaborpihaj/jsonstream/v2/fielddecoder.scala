package com.gaborpihaj.jsonstream.v2

import shapeless._
import shapeless.labelled.FieldType

trait Field {
  type Out
  def name: String
  def decoder: Decoder[Out]
}

// base idea from: https://stackoverflow.com/a/53438635
sealed trait FieldDecoder[T] {
  def fields: List[Field]
}

object FieldDecoder {
  implicit val hnilFieldDecoder: FieldDecoder[HNil] =
    new FieldDecoder[HNil] {
      val fields: List[Field] = List.empty
    }

  implicit def hconsFieldDecoder[K <: Symbol, V, T <: HList](implicit
    witness: Witness.Aux[K],
    d: Decoder[V],
    rest: FieldDecoder[T]
  ): FieldDecoder[FieldType[K, V] :: T] =
    new FieldDecoder[FieldType[K, V] :: T] {
      def fields: List[Field] =
        new Field {
          type Out = V
          val name = witness.value.name
          val decoder = d
        } :: rest.fields
    }

  implicit def genericFieldDecoder[T, G](implicit
    lg: LabelledGeneric.Aux[T, G],
    rest: FieldDecoder[G]
  ): FieldDecoder[T] = {
    val (_) = (lg) // To avoid unused variable warning / error
    new FieldDecoder[T] {
      val fields: List[Field] = rest.fields
    }
  }

}
