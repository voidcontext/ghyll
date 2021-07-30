package ghyll

import ghyll.auto.DerivedDecoder

trait DecoderInstances {
  inline given derived[F[_], A](using d: DerivedDecoder[F, A]): Decoder[F, A] = d
}
