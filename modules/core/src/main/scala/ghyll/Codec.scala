package ghyll

import com.google.gson.stream.{JsonReader, JsonWriter}

trait Codec[A] extends Encoder[A] with Decoder[A]

object Codec extends CodecInstances {
  def apply[A](implicit encoder: Encoder[A], decoder: Decoder[A]): Codec[A] =
    new Codec[A] {
      def encode(writer: JsonWriter, value: A): StreamingEncoderResult = encoder.encode(writer, value)

      def decode(reader: JsonReader): StreamingDecoderResult[A] = decoder.decode(reader)
    }
}
