package object ghyll {
  type StreamingDecoderResult[A] = Either[StreamingDecoderError, A]
}
