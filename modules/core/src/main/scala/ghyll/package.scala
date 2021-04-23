import cats.instances.either._
import cats.syntax.either._
import cats.syntax.functor._

package object ghyll extends Decoding {
  type StreamingDecoderResult[A] = Either[StreamingDecoderError, A]

  object StreamingDecoderResult {
    private[ghyll] def catchDecodingFailure[A](body: => A): StreamingDecoderResult[A] =
      Either.catchNonFatal(body).left.map[StreamingDecoderError](t => StreamingDecodingFailure(t.getMessage()))
  }

  type StreamingEncoderResult = Either[StreamingEncoderError, Unit]

  object StreamingEncoderResult {
    private[ghyll] def catchEncodingFailure[A](body: => A): StreamingEncoderResult =
      Either.catchNonFatal(body).left.map[StreamingEncoderError](t => StreamingEncodingFailure(t.getMessage())).void
  }

}
