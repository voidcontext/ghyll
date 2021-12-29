package object ghyll extends Decoding {

  type DecoderResult[A] = Either[StreamingDecoderError, A]
  type EncoderResult = Either[StreamingEncoderError, Unit]

  private[ghyll] type StreamingDecoderResult[F[_], A] = F[DecoderResult[A]]
  private[ghyll] type StreamingEncoderResult[F[_]] = F[EncoderResult]
}
