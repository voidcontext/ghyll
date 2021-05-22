package ghyll.auto

import ghyll.Decoder

abstract class DerivedDecoder[F[_], A] extends Decoder[F, A]

//object DerivedDecoder extends DerivedDecoderInstances {}
