package ghyll

import cats.effect.Sync
import java.io.OutputStream

private[ghyll] trait Encoding {
  def encode[F[_], A](value: A, outStream: OutputStream)(implicit
    encoder: Encoder[F, A],
    F: Sync[F]
  ): F[Unit] = ???
}
