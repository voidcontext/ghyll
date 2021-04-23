package ghyll.derivation

import ghyll._
import shapeless.Lazy

object auto {
  implicit def autoDeriveDecoder[A: DerivedDecoder]: Decoder[A] = deriveDecoder[A]
  implicit def autoDeriveEncoder[A: DerivedEncoder]: Encoder[A] = deriveEncoder[A]
  implicit def autoDeriveCodec[A](implicit d: Lazy[DerivedDecoder[A]], e: Lazy[DerivedEncoder[A]]): Codec[A] =
    deriveCodec[A](d.value, e.value)
}
