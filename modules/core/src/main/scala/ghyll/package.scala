import cats.Applicative
import cats.syntax.applicative._
import ghyll.utils.EitherOps

package object ghyll extends Decoding {

  type Json

  type DecoderResult[A] = Either[StreamingDecoderError, A]
  type EncoderResult = Either[StreamingEncoderError, Unit]

  private[ghyll] type StreamingDecoderResult[F[_], A] = F[DecoderResult[A]]
  private[ghyll] type StreamingEncoderResult[F[_]] = F[EncoderResult]

  private[ghyll] object StreamingDecoderResult {
    def wrapError[F[_]: Applicative, A](err: TokeniserError): StreamingDecoderResult[F, A] =
      StreamingDecoderError.wrapError(err).left[A].pure[F]

    def wrapError[F[_]: Applicative, A](err: StreamingDecoderError): StreamingDecoderResult[F, A] =
      err.left[A].pure[F]

  }
}
