package ghyll

import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName

sealed trait StreamingDecoderError
case object Unimplemented extends StreamingDecoderError
final case class StreamingDecodingFailure(message: String) extends StreamingDecoderError
final case class DecodingNestingerror(message: String) extends StreamingDecoderError

object StreamingDecoderError {
  private[ghyll] def wrapError[A](err: TokeniserError): StreamingDecoderError =
    StreamingDecodingFailure(s"Got error: $err")

  private[ghyll] def unexpectedToken(t: JsonToken, pos: List[Pos]): StreamingDecoderError =
    StreamingDecodingFailure(s"Unexpected token: ${TokenName(t).show()} at $pos")

  private[ghyll] def notExpectedToken[T <: JsonToken: TokenName](t: JsonToken, pos: List[Pos]): StreamingDecoderError =
    StreamingDecodingFailure(s"Expected ${TokenName[T].show()}, but got ${TokenName(t).show()} at $pos")
}
