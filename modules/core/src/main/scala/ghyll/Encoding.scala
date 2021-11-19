package ghyll

import java.io.{OutputStream, OutputStreamWriter}

import cats.effect.{Resource, Sync}
import cats.instances.option._
import cats.syntax.functor._
import cats.syntax.traverse._
import com.google.gson.stream.JsonWriter
import fs2.Stream
import ghyll.json.JsonToken
import ghyll.json.JsonToken._

private[ghyll] trait Encoding {
  def encode[F[_], A](value: A, outStream: OutputStream)(implicit
    encoder: Encoder[A],
    F: Sync[F]
  ): F[Unit] =
    (for {
      out    <- Resource.fromAutoCloseable[F, OutputStream](F.delay(outStream))
      writer <- Resource.fromAutoCloseable[F, JsonWriter](F.delay(new JsonWriter(new OutputStreamWriter(out))))
    } yield writer).use { case writer =>
      Stream
        .unfoldEval(encoder.encode(value)) { s =>
          s.headOption.traverse {
            case Right(token) => F.pure(token -> s.tail)
            case Left(err)    => F.raiseError[(JsonToken, StreamingEncoderResult)](new RuntimeException(err.toString()))
          }
        }
        .evalMap {
          case Str(string)           => F.delay(writer.value(string)).void
          case Number(v: Int)        => F.delay(writer.value(v.toLong)).void
          case Number(v: BigDecimal) => F.delay(writer.value(v)).void
          case Number(_)             => F.raiseError(new IllegalStateException("cannot encode number")).void
          case Boolean(bool)         => F.delay(writer.value(bool)).void
          case Null                  => F.delay(writer.nullValue()).void

          case BeginObject => F.delay(writer.beginObject()).void
          case Key(name)   => F.delay(writer.name(name)).void
          case EndObject   => F.delay(writer.endObject()).void
          case BeginArray  => F.delay(writer.beginArray()).void
          case EndArray    => F.delay(writer.endArray()).void
        }
        .compile
        .drain
    }
}
