package ghyll

import ghyll.auto.DerivedEncoder

trait EncoderInstances {
  inline given derived[F[_], A](using e: DerivedEncoder[F, A]): Encoder[F, A] = e
}
