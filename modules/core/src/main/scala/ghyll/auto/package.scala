package ghyll

import ghyll.auto.semi._

package object auto {
  implicit def autoDeriveDecoder[A](implicit d: DerivedDecoder[A]): Decoder[A] = deriveDecoder[A]
  implicit def autoDeriveEncoder[A](implicit e: DerivedEncoder[A]): Encoder[A] = deriveEncoder[A]
  implicit def autoDeriveCodec[A](implicit d: DerivedDecoder[A], e: DerivedEncoder[A]): Codec[A] =
    deriveCodec[A]
}
