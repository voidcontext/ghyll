package ghyll

import cats.effect.Sync

package object syntax {
  implicit class EncodingOps[A](value: A) extends Encoding {
    def asJsonString[F[_]: Sync](implicit encoder: Encoder[F, A]): F[String] = ???
  }
}
