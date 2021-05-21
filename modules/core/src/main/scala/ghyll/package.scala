import cats.ApplicativeError
import fs2.Stream
import ghyll.json.JsonToken
//import cats.syntax.either._

package object ghyll extends Decoding {
  type TokenStream[F[_]] = Stream[F, JsonToken]

  object TokenStream {
    def skipValue[F[_]](stream: TokenStream[F])(implicit ae: ApplicativeError[F, Throwable]): TokenStream[F] = {
      def skip(stream: TokenStream[F], stack: List[JsonToken]): TokenStream[F] =
        stream.head.flatMap {
          case t @ (JsonToken.BeginArray | JsonToken.BeginObject) => skip(stream.tail, t :: stack)
          case JsonToken.Null | JsonToken.Str(_) | JsonToken.Number(_) | JsonToken.Boolean(_) =>
            if (stack.isEmpty) stream.tail
            else skip(stream.tail, stack)
          case JsonToken.EndArray if (stack.headOption.exists(_ == JsonToken.BeginArray)) =>
            if (stack.isEmpty) stream.tail
            else skip(stream.tail, stack.tail)
          case JsonToken.EndObject if (stack.headOption.exists(_ == JsonToken.BeginObject)) =>
            if (stack.isEmpty) stream.tail
            else skip(stream.tail, stack.tail)
          case _ => Stream.raiseError(new IllegalStateException("Something went wrong!"))
        }

      skip(stream, List.empty)
    }
  }

  type DecoderResult[A] = Either[StreamingDecoderError, A]
  type EncoderResult = Either[StreamingEncoderError, Unit]

  type StreamingDecoderResult[F[_], A] = Stream[F, Either[StreamingDecoderError, (A, Stream[F, JsonToken])]]
  type StreamingEncoderResult[F[_]] = Either[StreamingEncoderError, Stream[F, JsonToken]]
}
 
