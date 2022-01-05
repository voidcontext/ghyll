package ghyll

import ghyll.json.{JsonTokenReader, JsonTokenWriter}

trait Codec[F[_], A] extends Decoder[F, A] with Encoder[F, A]

object Codec {
  def apply[F[_], A](implicit decoder: Decoder[F, A], encoder: Encoder[F, A]): Codec[F, A] =
    new Codec[F, A] {
      def decode(reader: JsonTokenReader[F]): StreamingDecoderResult[F, A] = decoder.decode(reader)

      def encode(value: A, writer: JsonTokenWriter[F]): StreamingEncoderResult[F] = encoder.encode(value, writer)
    }

  implicit def codec[F[_], A](implicit decoder: Decoder[F, A], encoder: Encoder[F, A]): Codec[F, A] =
    apply[F, A](decoder, encoder)
}
