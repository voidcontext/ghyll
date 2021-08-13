package ghyll

import java.time.LocalDate

import scala.reflect.ClassTag

import cats.syntax.either._
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName

trait Decoder[A] {
  def decode(stream: TokenStream): StreamingDecoderResult[A]
}

object Decoder {
  def apply[A](implicit ev: Decoder[A]) = ev

  implicit def stringDecoder: Decoder[String] =
    createDecoder[String, JsonToken.Str] { case JsonToken.Str(v) => Right(v) }

  implicit def intDecoder: Decoder[Int] =
    createDecoder[Int, JsonToken.Number[String]] { case JsonToken.Number(v: String) =>
      Right(v.toInt)
    }

  implicit def booleanDecoder: Decoder[Boolean] =
    createDecoder[Boolean, JsonToken.Boolean] { case JsonToken.Boolean(v) => Right(v) }

  implicit def bigDecimalDecoder: Decoder[BigDecimal] =
    createDecoder[BigDecimal, JsonToken.Number[String]] { case JsonToken.Number(v: String) =>
      Right(BigDecimal(v))
    }

  implicit def localDateDecoder: Decoder[LocalDate] =
    createDecoder[LocalDate, JsonToken.Str] { case JsonToken.Str(v) => catchDecodingFailure(LocalDate.parse(v)) }

  implicit def optionDecoder[A](implicit aDecoder: Decoder[A]): Decoder[Option[A]] =
    _ match {
      case Right(JsonToken.Null -> _) #:: tail =>
        Right(Option.empty[A] -> tail)
      case head #:: tail =>
        aDecoder.decode(head #:: tail).map { case (value, tail) => Some(value) -> tail}
      case _ => Left(StreamingDecodingFailure("Unimplemented"))
    }

  implicit def listDecoder[A](implicit aDecoder: Decoder[A]): Decoder[List[A]] =
      withExpected[List[A], JsonToken.BeginArray](_) { case ((_: JsonToken.BeginArray, _), tail) =>
        def decodeNext(stream: TokenStream, result: List[A]): StreamingDecoderResult[List[A]] =
          stream match {
            case Right(JsonToken.EndArray -> _) #:: tail => Right(result -> tail)
            case head #:: tail =>
              aDecoder.decode(head #:: tail).flatMap {
                case value -> tail => decodeNext(tail, value :: result)
              }
          }

        decodeNext(tail, Nil).map {
          case (list, tail) => list.reverse -> tail
        }
      }

  implicit def mapDecoder[V](implicit valueDecoder: Decoder[V]): Decoder[Map[String, V]] =
    stream =>
      withExpected[Map[String, V], JsonToken.BeginObject](stream) { case ((_: JsonToken.BeginObject, _) -> tail) =>
        def decodeNext(stream: TokenStream, result: Map[String, V]): StreamingDecoderResult[Map[String, V]] =
          stream match {
            case Right(JsonToken.EndObject -> _) #:: tail => Right(result -> tail)
            case Right(JsonToken.Key(key) -> _) #:: tail =>
              valueDecoder.decode(tail).flatMap {
                case value -> remaining =>
                  decodeNext(remaining, result + (key -> value))
              }
            case Right(token -> pos) #:: _ =>
              Left(
                StreamingDecodingFailure(
                  s"Expected ${TokenName[JsonToken.Key].show()} or ${TokenName[JsonToken.EndObject]} but got ${TokenName(token).show()} at $pos"
                )
              )
            case Left(err) #:: _ => Left(StreamingDecodingFailure(err.toString))
          }

        decodeNext(tail, Map.empty)
      }

  private def createDecoder[A, Token <: JsonToken: ClassTag: TokenName](
    decodeToken: Token => Either[StreamingDecoderError, A]
  ): Decoder[A] =
    new Decoder[A] {
      def decode(stream: TokenStream): StreamingDecoderResult[A] =
        stream match {
          case Right((token: Token) -> _) #:: tail =>
            decodeToken(token).map(_ -> tail)
          case Right(t -> pos) #:: _ =>
            Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got ${TokenName(t).show()} at $pos"))
          case Left(err) #:: _ =>
            Left(StreamingDecodingFailure(err.toString))
          case LazyList() =>
            Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got 'END DOCUMENT'"))
        }

    }

  private[ghyll] def withExpected[A, Token <: JsonToken: TokenName](
    stream: TokenStream
  )(
    pf: PartialFunction[((JsonToken, List[Pos]), TokenStream), StreamingDecoderResult[A]]
  ): StreamingDecoderResult[A] =
    stream match {
      case Right(head) #:: tail =>
        pf.orElse(expected[A, Token])(head -> tail)
      case LazyList() =>
        endOfDocumentError[A, Token]
    }

  private def endOfDocumentError[A, Token <: JsonToken: TokenName]: StreamingDecoderResult[A] =
    Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()} but got 'EndOfDocument'"))

  private def expected[A, Token <: JsonToken: TokenName]
    : PartialFunction[((JsonToken, List[Pos]), TokenStream), StreamingDecoderResult[A]] = { case ((t, pos), _) =>
      Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got ${TokenName(t).show()} at $pos"))
  }

  // TODO: make sure pos is accessible here
  private def catchDecodingFailure[A](body: => A): Either[StreamingDecoderError, A] =
    Either.catchNonFatal(body).left.map[StreamingDecoderError](t => StreamingDecodingFailure(t.getMessage()))

}
