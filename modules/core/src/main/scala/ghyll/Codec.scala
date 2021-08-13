package ghyll

trait Codec[A] extends Decoder[A] with Encoder[A]

object Codec {
  def apply[A](implicit decoder: Decoder[A], encoder: Encoder[A]): Codec[A] =
    new Codec[A] {
      def decode(stream: TokenStream): StreamingDecoderResult[A] = decoder.decode(stream)

      def encode(value: A): StreamingEncoderResult = encoder.encode(value)
    }
}
