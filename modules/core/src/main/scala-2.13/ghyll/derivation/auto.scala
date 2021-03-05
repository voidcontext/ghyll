package ghyll.derivation

import ghyll.Decoder

object auto {
  implicit def autoDerive[A](implicit d: DerivedDecoder[A]): Decoder[A] = ghyll.deriveDecoder[A]
}
