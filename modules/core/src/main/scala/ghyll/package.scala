import ghyll.json.JsonToken

package object ghyll extends Decoding {
//  type TokenStream[F[_]] = Stream[F, (JsonToken, List[Pos])]

  type TokenStream = LazyList[Either[TokeniserError, (JsonToken, List[Pos])]]

  type DecoderResult[A] = Either[StreamingDecoderError, A]
//  type EncoderResult = Either[StreamingEncoderError, Unit]

  private[ghyll] type StreamingDecoderResult[A] = Either[StreamingDecoderError, (A, TokenStream)]
  private[ghyll] type StreamingEncoderResult = LazyList[Either[StreamingEncoderError, JsonToken]]
}
