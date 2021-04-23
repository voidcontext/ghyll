package ghyll

import java.io.ByteArrayOutputStream

import cats.effect.{Resource, Sync}
import cats.syntax.functor._

package object syntax {
  implicit class EncodingOps[A: Encoder](value: A) extends Encoding {
    def asJsonString[F[_]: Sync]: F[String] =
      Resource
        .fromAutoCloseable(Sync[F].delay(new ByteArrayOutputStream()))
        .flatMap(out => encode(value, out).as(out))
        .use(Sync[F].pure)
        .map(_.toString())
  }
}
