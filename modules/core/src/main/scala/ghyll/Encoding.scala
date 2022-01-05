package ghyll

import java.io.{OutputStream, OutputStreamWriter}

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.gson.stream.JsonWriter
import ghyll.json.JsonTokenWriter

private[ghyll] trait Encoding {
  def encode[F[_], A](value: A, outStream: OutputStream)(implicit
    encoder: Encoder[F, A],
    F: Sync[F]
  ): F[Unit] =
    (for {
      out    <- Resource.fromAutoCloseable[F, OutputStream](F.delay(outStream))
      writer <- Resource.fromAutoCloseable[F, JsonWriter](F.delay(new JsonWriter(new OutputStreamWriter(out))))
    } yield writer).map(JsonTokenWriter(_)).use { tokenWriter =>
      encoder
        .encode(value, tokenWriter)
        .map(_.left.map(err => new RuntimeException(err.toString)))
        .flatMap(F.fromEither)

    }
}
