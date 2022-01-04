package ghyll.json

import cats.effect.{Ref, Sync}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.google.gson.stream.{JsonReader => GsonReader, JsonToken => GsonToken}
import ghyll.utils.EitherOps
import ghyll.{Pos, TokeniserError, UnimplementedToken}

trait JsonTokenReader[F[_]] {
  def next: F[ReadResult]
}

object JsonTokenReader {
  def apply[F[_]](reader: GsonReader)(implicit F: Sync[F]): F[JsonTokenReader[F]] =
    Ref.of[F, List[Pos]](Nil).map { stackRef =>
      new JsonTokenReader[F] {
        def next =
          (
            for {
              stack      <- stackRef.get
              readResult <- read(reader).map(_.map(token => JsonToken.pos(token, stack) -> token))
              _          <- readResult.fold(_ => F.unit, r => stackRef.set(r._1))
            } yield readResult
          )

      }
    }

  def prepend[F[_]: Sync](token: (List[Pos], JsonToken), reader: JsonTokenReader[F]): F[JsonTokenReader[F]] =
    Ref.of[F, Option[(List[Pos], JsonToken)]](Option(token)).map { prependedRef =>
      new JsonTokenReader[F] {
        def next =
          for {
            maybePrepended <- prependedRef.getAndSet(None)
            n              <- maybePrepended.fold(reader.next)(_.right[TokeniserError].pure[F])
          } yield n
      }
    }

  private def read[F[_]](reader: GsonReader)(implicit F: Sync[F]): F[Either[TokeniserError, JsonToken]] =
    F.delay {
      reader.peek() match {
        case GsonToken.BEGIN_ARRAY  =>
          reader.beginArray()
          Right(JsonToken.BeginArray)
        case GsonToken.END_ARRAY    =>
          reader.endArray()
          Right(JsonToken.EndArray)
        case GsonToken.BEGIN_OBJECT =>
          reader.beginObject()
          Right(JsonToken.BeginObject)
        case GsonToken.END_OBJECT   =>
          reader.endObject()
          Right(JsonToken.EndObject)
        case GsonToken.NUMBER       =>
          Right(JsonToken.Number(reader.nextString()))
        case GsonToken.STRING       =>
          Right(JsonToken.Str(reader.nextString()))
        case GsonToken.NAME         =>
          Right(JsonToken.Key(reader.nextName()))
        case GsonToken.BOOLEAN      =>
          Right(JsonToken.Boolean(reader.nextBoolean()))
        case GsonToken.NULL         =>
          reader.nextNull()
          Right(JsonToken.Null)
        case _                      => Left(UnimplementedToken)
      }
    }
}
