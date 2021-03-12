import ghyll.derivation.Derivation

package object ghyll extends Derivation with DecodeFunctions {
  type StreamingDecoderResult[A] = Either[StreamingDecoderError, A]
}
