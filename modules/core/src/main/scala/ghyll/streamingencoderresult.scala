package ghyll

sealed trait StreamingEncoderError
case object InternalResult extends StreamingEncoderError
final case class StreamingEncodingFailure(message: String) extends StreamingEncoderError
