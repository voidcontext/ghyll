package ghyll.json

import cats.effect.Sync
import cats.syntax.functor._
import com.google.gson.stream.JsonWriter
import ghyll.json.JsonToken._

trait JsonTokenWriter[F[_]] {
  def write(token: JsonToken): F[Unit]
}

object JsonTokenWriter {
  def apply[F[_]: Sync](writer: JsonWriter): JsonTokenWriter[F] =
    new JsonTokenWriter[F] {
      @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
      def write(token: JsonToken): F[Unit] =
        Sync[F].delay {
          token match {
            case Str(string)           => writer.value(string)
            case Number(v: Int)        => writer.value(v.toLong)
            case Number(v: BigDecimal) => writer.value(v)
            case Number(v)             => throw new IllegalStateException(s"cannot encode number: $v (${v.getClass()})")
            case Boolean(bool)         => writer.value(bool)
            case Null                  => writer.nullValue()

            case BeginObject => writer.beginObject()
            case Key(name)   => writer.name(name)
            case EndObject   => writer.endObject()
            case BeginArray  => writer.beginArray()
            case EndArray    => writer.endArray()
          }
        }.as(())

    }
}
