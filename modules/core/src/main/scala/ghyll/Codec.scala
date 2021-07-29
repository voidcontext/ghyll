package ghyll

trait Codec[F[_], A] extends Encoder[F, A] with Decoder[F, A]

object Codec extends CodecInstances {
  def apply[F[_], A](implicit decoder: Decoder[F, A], encoder: Encoder[F, A]): Codec[F, A] =
    new Codec[F, A] {
      def encode(value: A): StreamingEncoderResult[F] = encoder.encode(value)

      def decode(stream: TokenStream[F]): StreamingDecoderResult[F, A] = decoder.decode(stream)
    }
}
