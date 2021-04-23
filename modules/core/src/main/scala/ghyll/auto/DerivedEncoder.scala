package ghyll.auto

import ghyll.Encoder

abstract class DerivedEncoder[A] extends Encoder[A]

object DerivedEncoder extends DerivedEncoderInstances {}
