package ghyll

import ghyll.auto.{DerivedDecoder, DerivedEncoder}

trait CodecInstances {
  inline given derived[A: DerivedDecoder: DerivedEncoder]: Codec[A] = Codec[A]
}
