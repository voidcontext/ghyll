package ghyll

import ghyll.auto.{DerivedDecoder, DerivedEncoder}

trait CodecInstances {
  inline given derived[F[_], A](using d: DerivedDecoder[F, A], e: DerivedEncoder[F, A]): Codec[F, A] = Codec[F, A]
}
