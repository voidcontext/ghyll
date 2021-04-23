package ghyll.auto

import ghyll.{Codec, Decoder, Encoder}

package object semi {
  def deriveDecoder[A](implicit d: DerivedDecoder[A]): Decoder[A] = d

  def deriveEncoder[A](implicit e: DerivedEncoder[A]): Encoder[A] = e

  def deriveCodec[A](implicit d: DerivedDecoder[A], e: DerivedEncoder[A]): Codec[A] = Codec[A](d, e)
}
