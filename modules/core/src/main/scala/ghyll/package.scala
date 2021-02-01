import ghyll.derivation.Derivation

package object ghyll extends Derivation with Decode {
  type StreamingDecoderResult[A] = Either[StreamingDecoderError, A]
}
