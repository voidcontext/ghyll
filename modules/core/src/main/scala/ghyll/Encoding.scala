package ghyll

import java.io.OutputStream

import cats.effect.Sync

private[ghyll] trait Encoding {
  def encode[F[_], A](value: A, outStream: OutputStream)(implicit
    encoder: Encoder[F, A],
    F: Sync[F]
  ): F[Unit] = ???
}
