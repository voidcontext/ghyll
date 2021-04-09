package ghyll.derivation

import ghyll._

object auto {
  implicit def autoDeriveDecoder[A](implicit d: DerivedDecoder[A]): Decoder[A] = deriveDecoder[A]
  implicit def autoDeriveEncoder[A](implicit d: DerivedEncoder[A]): Encoder[A] = deriveEncoder[A]
}
