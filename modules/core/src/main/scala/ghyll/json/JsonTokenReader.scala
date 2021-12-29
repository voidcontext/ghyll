package ghyll.json

import cats.effect.Sync
import com.google.gson.stream.{JsonReader => GsonReader, JsonToken => GsonToken}
import ghyll.TokeniserError
import ghyll.UnimplementedToken
import cats.syntax.functor._
import ghyll.Pos
import cats.effect.Ref
import cats.data.EitherT
import ghyll.json.JsonTokenReader.JsonTokenReaderResult

trait JsonTokenReader[F[_]] {
  def next: F[Either[TokeniserError, (List[Pos], JsonTokenReaderResult)]]
  def peek: F[Either[TokeniserError, (List[Pos], JsonToken)]]
}

object JsonTokenReader {
  type JsonTokenReaderResult = Either[JsonToken.Delimiter, JsonValue]

  private final case class ReadResult[F[_]](
    token: JsonToken,
    stack:  List[Pos],
    consume:  F[JsonTokenReaderResult]
  )

  implicit class RightOps[A](value: A) {
    def right[L]: Either[L, A] =
      Right(value)
  }

  def apply[F[_]](reader: GsonReader)(implicit F: Sync[F]): F[JsonTokenReader[F]] =
    Ref.of[F, List[Pos]](Nil).map {stackRef =>
      new JsonTokenReader[F] {
        def next =
          (
            for {
              stack <- EitherT.liftF[F, TokeniserError, List[Pos]](stackRef.get)
              readResult <- EitherT.apply(read(reader, stack))
              _ <- EitherT.liftF[F, TokeniserError, Unit](stackRef.set(readResult.stack))
              result <- EitherT.liftF[F, TokeniserError, JsonTokenReaderResult](readResult.consume)
            } yield (readResult.stack, result)
          ).value

        def peek =
          (
            for {
              stack <- EitherT.liftF[F, TokeniserError, List[Pos]](stackRef.get)
              readResult <- EitherT.apply(read(reader, stack))
            } yield (readResult.stack, readResult.token)
          ).value
      }
    }

  private def read[F[_]](reader: GsonReader, stack: List[Pos])(implicit F: Sync[F]): F[Either[TokeniserError, ReadResult[F]]] =
    F.delay {
      reader.peek() match {
        case GsonToken.BEGIN_ARRAY  =>
          ReadResult[F](
            token = JsonToken.BeginArray,
            stack = Pos.ArrayIndex(0) :: Pos.Array :: stack,
            consume = F.delay { reader.beginArray(); Left(JsonToken.BeginArray) }
          ).right
        case GsonToken.END_ARRAY    =>
          ReadResult[F](
            token = JsonToken.EndArray,
            stack = Pos.endArray(stack),
            consume = F.delay { reader.endArray(); Left(JsonToken.EndArray) }
          ).right
        case GsonToken.BEGIN_OBJECT =>
          ReadResult[F](
            token = JsonToken.BeginObject,
            stack = Pos.Obj :: stack,
            consume = F.delay { reader.beginObject(); Left(JsonToken.BeginObject) }
          ).right
        case GsonToken.END_OBJECT   =>
          ReadResult[F](
            token = JsonToken.EndObject,
            stack = Pos.endObj(stack),
            consume = F.delay { reader.endObject(); Left(JsonToken.EndObject) }
          ).right
        case GsonToken.NUMBER       => 
          ReadResult[F](
            token = JsonToken.Number,
            stack = Pos.nextPos(stack),
            consume = F.delay(Right(JsonValue.Number(reader.nextString())))
          ).right
        case GsonToken.STRING       => 
          ReadResult[F](
            token = JsonToken.Str,
            stack = Pos.nextPos(stack),
            consume = F.delay(Right(JsonValue.Str(reader.nextString())))
          ).right
        case GsonToken.NAME         => 
          ReadResult[F](
            token = JsonToken.Key,
            stack = Pos.nextKey(stack, "???"),
            consume = F.delay(Right(JsonValue.Key(reader.nextName())))
          ).right
        case GsonToken.BOOLEAN      => 
          ReadResult[F](
            token = JsonToken.Boolean,
            stack = Pos.nextPos(stack),
            consume = F.delay(Right(JsonValue.Boolean(reader.nextBoolean())))
          ).right
        case GsonToken.NULL         =>
          ReadResult[F](
            token = JsonToken.Null,
            stack = Pos.nextPos(stack),
            consume = F.delay {reader.nextNull(); Right(JsonValue.Null)}
          ).right
        case _                      => Left(UnimplementedToken)
      }
    }
}
