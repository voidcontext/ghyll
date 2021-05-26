package ghyll

import fs2.Stream
import ghyll.json.JsonToken

trait Codec[F[_], A] extends Encoder[F, A] with Decoder[F, A]

object Codec extends CodecInstances {
  def apply[F[_], A](implicit decoder: Decoder[F, A], encoder: Encoder[F, A]): Codec[F, A] =
    new Codec[F, A] {
      def encode(stream: Stream[F, JsonToken], value: A): StreamingEncoderResult[F] = encoder.encode(stream, value)

      def decode(stream: TokenStream[F]): StreamingDecoderResult[F, A] = decoder.decode(stream)
    }
}
