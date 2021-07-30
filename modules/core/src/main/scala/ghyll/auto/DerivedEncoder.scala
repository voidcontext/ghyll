package ghyll.auto

import ghyll.Encoder

abstract class DerivedEncoder[F[_], A] extends Encoder[F, A]

object DerivedEncoder extends DerivedEncoderInstances {}
