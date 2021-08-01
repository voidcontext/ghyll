package ghyll

trait Codec[F[_], A] extends Decoder[F, A] with Encoder[F, A]

object Codec {
  def apply[F[_], A](implicit decoder: Decoder[F, A], encoder: Encoder[F, A]): Codec[F, A] =
    new Codec[F, A] {
      def decode(stream: TokenStream[F]): StreamingDecoderResult[F, A] = decoder.decode(stream)

      def encode(value: A): StreamingEncoderResult[F] = encoder.encode(value)
    }
}
