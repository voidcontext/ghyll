package ghyll

trait StreamingEncoderError
final case class StreamingEncodingFailure(message: String) extends StreamingEncoderError
