import fs2.Stream
import ghyll.json.JsonToken

package object ghyll extends Decoding {
  type TokenStream[F[_]] = Stream[F, (JsonToken, List[Pos])]

  type DecoderResult[A] = Either[StreamingDecoderError, A]
  type EncoderResult = Either[StreamingEncoderError, Unit]

  type StreamingDecoderResult[F[_], A] = Stream[F, Either[StreamingDecoderError, (A, TokenStream[F])]]
  type StreamingEncoderResult[F[_]] = Either[StreamingEncoderError, Stream[F, JsonToken]]
}
