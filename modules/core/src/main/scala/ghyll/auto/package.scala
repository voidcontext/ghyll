package ghyll

//import ghyll.auto.semi._

package object auto extends DerivedDecoderInstances with DerivedEncoderInstances {
  // implicit def autoDeriveDecoder[F[_], A](implicit d: DerivedDecoder[F, A]): Decoder[F, A] = deriveDecoder[F, A]
  // implicit def autoDeriveEncoder[F[_], A](implicit e: DerivedEncoder[F, A]): Encoder[F, A] = deriveEncoder[F, A]
  // implicit def autoDeriveCodec[F[_], A](implicit d: DerivedDecoder[F, A], e: DerivedEncoder[F, A]): Codec[F, A] =
  //   deriveCodec[F, A](d, e)
}
