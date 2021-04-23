package ghyll.derivation

import ghyll.{Codec, Decoder, Encoder}

private[ghyll] trait Derivation {
  def deriveDecoder[A](implicit d: DerivedDecoder[A]): Decoder[A] = d

  def deriveEncoder[A](implicit e: DerivedEncoder[A]): Encoder[A] = e

  def deriveCodec[A: DerivedDecoder: DerivedEncoder]: Codec[A] = Codec[A]
}
