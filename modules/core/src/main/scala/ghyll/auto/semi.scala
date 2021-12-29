package ghyll.auto

import ghyll.{Codec, Decoder, Encoder}

object semi {
  def deriveDecoder[F[_], A](implicit d: DerivedDecoder[F, A]): Decoder[F, A] = d

  def deriveEncoder[F[_], A](implicit e: DerivedEncoder[F, A]): Encoder[F, A] = e

  def deriveCodec[F[_], A](implicit d: DerivedDecoder[F, A], e: DerivedEncoder[F, A]): Codec[F, A] = Codec[F, A](d, e)

}
