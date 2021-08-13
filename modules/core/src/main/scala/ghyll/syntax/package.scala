package ghyll

import java.io.ByteArrayOutputStream

import cats.effect.{Resource, Sync}
import cats.syntax.functor._

package object syntax {
  implicit class EncodingOps[A](value: A) extends Encoding {
    def asJsonString[F[_]: Sync](implicit encoder: Encoder[A]): F[String] =
      Resource
        .fromAutoCloseable(Sync[F].delay(new ByteArrayOutputStream()))
        .use(out => encode(value, out).as(out))
        .map(_.toString())
  }
}
