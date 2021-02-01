package ghyll.derivation

import ghyll.Decoder

private[ghyll] trait Derivation {
  def deriveDecoder[A](implicit d: DerivedDecoder[A]): Decoder[A] = d
}
