package ghyll

import ghyll.auto.semi._

package object auto {
  implicit def autoDeriveDecoder[A: DerivedDecoder]: Decoder[A] = deriveDecoder[A]
  implicit def autoDeriveEncoder[A: DerivedEncoder]: Encoder[A] = deriveEncoder[A]
  implicit def autoDeriveCodec[A](implicit d: DerivedDecoder[A], e: DerivedEncoder[A]): Codec[A] =
    deriveCodec[A](d, e)
}
