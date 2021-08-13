package ghyll

// import java.io.{InputStream, InputStreamReader}
import java.io.{InputStream}

//import cats.ApplicativeError
import cats.effect.kernel.{Resource, Sync}
import ghyll.json.JsonToken
import cats.syntax.eq._
// import com.google.gson.stream.{JsonReader, JsonToken => GsonToken}
// import fs2.{Pull, Stream}
// import ghyll.gson.Implicits._
// import ghyll.json.JsonToken
import ghyll.json.JsonToken._

object TokenStream {

  def fromJson[F[_]: Sync](json: InputStream): Resource[F, TokenStream] = ???
    // readerResource(json)
    //   .map(Stream.unfoldEval(_)(nextToken[F]).withPos)

  // @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
  // private def nextToken[F[_]: Sync](reader: JsonReader): F[Option[(JsonToken, JsonReader)]] =
  //   Sync[F].delay {
  //     if (reader.peek() === GsonToken.END_DOCUMENT) None
  //     else
  //       Option(
  //         (reader.peek() match {
  //           case GsonToken.BEGIN_ARRAY  =>
  //             reader.beginArray()
  //             JsonToken.BeginArray
  //           case GsonToken.END_ARRAY    =>
  //             reader.endArray()
  //             JsonToken.EndArray
  //           case GsonToken.BEGIN_OBJECT =>
  //             reader.beginObject()
  //             JsonToken.BeginObject
  //           case GsonToken.END_OBJECT   =>
  //             reader.endObject()
  //             JsonToken.EndObject
  //           case GsonToken.NUMBER       => JsonToken.Number(reader.nextString())
  //           case GsonToken.STRING       => JsonToken.Str(reader.nextString())
  //           case GsonToken.NAME         => JsonToken.Key(reader.nextName())
  //           case GsonToken.BOOLEAN      => JsonToken.Boolean(reader.nextBoolean())
  //           case GsonToken.NULL         =>
  //             reader.nextNull()
  //             JsonToken.Null
  //           case _                      => throw new RuntimeException("Unimplemented")
  //         }) -> reader
  //       )
  //   }

  // private def readerResource[F[_]: Sync](json: InputStream): Resource[F, JsonReader] =
  //   Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(json))))

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
      stream match {
        case Right(head) #:: tail =>
          val current: List[Pos] = currentPos(head, pos)
          Right(head -> current)  #:: addPos(tail, current)
        case Left(err) #:: _ =>
          LazyList(Left(err))
        case LazyList() => LazyList.empty

      }

    addPos(stream, List.empty)
  }

  // implicit class StreamOps(stream: Stream[F, JsonToken]) {
  //   def withPos: TokenStream = TokenStream.withPos(stream)
  // }

  def skipValue[F[_]](stream: TokenStream): TokenStream = {
    def skip(stream: TokenStream, stack: List[JsonToken]): TokenStream =
      stream match {
          case Right((t @ (BeginArray | BeginObject)) -> _) #:: tail                            => skip(tail, t :: stack)
          case Right((Null | Str(_) | Number(_) | Boolean(_) | Key(_)) -> _) #:: tail           =>
            if (stack.isEmpty) tail
            else skip(tail, stack)
          case Right(EndArray -> _) #:: tail if (stack.headOption.exists(_ === BeginArray))   =>
            if (stack.tail.isEmpty) tail
            else skip(tail, stack.tail)
          case Right(EndObject -> _) #:: tail if (stack.headOption.exists(_ === BeginObject)) =>
            if (stack.tail.isEmpty) tail
            else skip(tail, stack.tail)
          case Right(t -> p) #:: _                                                               =>
           LazyList(Left(NestingError(s"Nesting Error: got ${TokenName(t).show()} at $p")))
        case Left(err) #:: _ =>
          LazyList(Left(err))

      }
    skip(stream, List.empty)
  }
}
