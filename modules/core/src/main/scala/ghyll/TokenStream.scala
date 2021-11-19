package ghyll

import java.io.{InputStream, InputStreamReader}

//import cats.Applicative
import cats.effect.kernel.{Resource, Sync}
import cats.syntax.eq._
import com.google.gson.stream.{JsonReader, JsonToken => GsonToken}
import ghyll.gson.Implicits._
import ghyll.json.JsonToken
import ghyll.json.JsonToken._

object TokenStream {

  def fromJson[F[_]: Sync](json: InputStream): Resource[F, TokenStream] =
    readerResource(json)
      .flatMap(reader => Resource.eval(readInput(reader)))

  private def readInput[F[_]: Sync](reader: JsonReader): F[TokenStream] =
    Sync[F].delay {

      def readNext: Either[TokeniserError, JsonToken] =
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
            case GsonToken.NUMBER       => Right(JsonToken.Number(reader.nextString()))
            case GsonToken.STRING       => Right(JsonToken.Str(reader.nextString()))
            case GsonToken.NAME         => Right(JsonToken.Key(reader.nextName()))
            case GsonToken.BOOLEAN      => Right(JsonToken.Boolean(reader.nextBoolean()))
            case GsonToken.NULL         =>
              reader.nextNull()
              Right(JsonToken.Null)
            case _                      => Left(UnimplementedToken)
          }

      def nextToken: LazyList[Either[TokeniserError, JsonToken]] =
        if (reader.peek() === GsonToken.END_DOCUMENT) LazyList.empty
        else {
          readNext #:: nextToken
        }

      withPos(nextToken)
    }

  private def readerResource[F[_]: Sync](json: InputStream): Resource[F, JsonReader] =
    Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(json))))

  def withPos[F[_]](stream: LazyList[Either[TokeniserError, JsonToken]]): TokenStream = {
    def currentPos(token: JsonToken, pos: List[Pos]): List[Pos] =
      token match {
        case BeginArray                             => Pos.ArrayIndex(0) :: Pos.Array :: pos
        case EndArray                               => Pos.endArray(pos)
        case BeginObject                            => Pos.Obj :: pos
        case EndObject                              => Pos.endObj(pos)
        case Number(_) | Str(_) | Boolean(_) | Null => Pos.nextPos(pos)
        case Key(n)                                 => Pos.nextKey(pos, n)
      }

    def addPos(stream: LazyList[Either[TokeniserError, JsonToken]], pos: List[Pos]): TokenStream =
      stream.headOption.fold[TokenStream](LazyList.empty) {
        _.fold(
          err => LazyList(Left(err)),
          head => Right(head -> pos) #:: addPos(stream.tail, currentPos(head, pos))
        )
      }
   

   ((Left[TokeniserError, (JsonToken, List[Pos])](LazyHead)) #:: addPos(stream, List.empty)).tail
  }

  // implicit class StreamOps(stream: Stream[F, JsonToken]) {
  //   def withPos: TokenStream = TokenStream.withPos(stream)
  // }

  def skipValue[F[_]](stream: TokenStream): TokenStream = {

    def skip(s: TokenStream, stack: List[JsonToken]): TokenStream =
      s.headOption
        .map(
          _.fold(
            err => LazyList(Left(err)),
            {
              case (t @ (BeginArray | BeginObject)) -> _                          => skip(s.tail, t :: stack)
              case (Null | Str(_) | Number(_) | Boolean(_) | Key(_)) -> _         =>
                if (stack.isEmpty) s.tail
                else skip(s.tail, stack)
              case EndArray -> _ if (stack.headOption.exists(_ === BeginArray))   =>
                if (stack.tail.isEmpty) s.tail
                else skip(s.tail, stack.tail)
              case EndObject -> _ if (stack.headOption.exists(_ === BeginObject)) =>
                if (stack.tail.isEmpty) s.tail
                else skip(s.tail, stack.tail)
              case t -> p                                                         =>
                LazyList(Left(NestingError(s"Nesting Error: got ${TokenName(t).show()} at $p")))
            }
          )
        )
        .getOrElse(
          if (stack.isEmpty) LazyList.empty
          else LazyList(Left(NestingError("Expected a token, but stream is empty")))
        )

    skip(stream, List.empty)
  }

  // object syntax {
  //   implicit class TokenStreamOps(t: TokenStream) {
  //     def withNext[F[_], E, A](ifEmpty: E, ef: TokeniserError => E)(f: ((JsonToken, List[Pos])) => F[Either[E, A]])(
  //       implicit F: Applicative[F]
  //     ): F[Either[E, A]] =
  //       t.headOption.map(_.fold[F[Either[E, A]]](te => F.pure(Left(ef(te))), f)).getOrElse(F.pure(Left(ifEmpty)))
  //   }
  // }
}
