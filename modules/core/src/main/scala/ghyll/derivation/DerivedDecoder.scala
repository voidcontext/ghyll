package ghyll.derivation

import ghyll.Decoder

abstract class DerivedDecoder[A] extends Decoder[A]

object DerivedDecoder extends DerivedDecoderInstances {}
