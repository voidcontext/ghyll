package ghyll

import ghyll.derivation.{DerivedDecoder, DerivedEncoder}

trait CodecInstances {
  inline given derived[A: DerivedDecoder: DerivedEncoder]: Codec[A] = Codec[A]
}
