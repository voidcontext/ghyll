package ghyll

sealed trait StreamingDecoderError
case object Unimplemented extends StreamingDecoderError
final case class StreamingDecodingFailure(message: String) extends StreamingDecoderError
