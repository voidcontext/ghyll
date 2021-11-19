package ghyll

import java.time.LocalDate

import scala.reflect.ClassTag

//import cats.{Eval, Id}
import cats.syntax.either._
// import ghyll.TokenStream.syntax._
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName
//import scala.annotation.tailrec
import cats.Eval

trait Decoder[A] {
  def decode(stream: TokenStream): StreamingDecoderResult[A]
}

object Decoder {
  def apply[A](implicit ev: Decoder[A]) = ev

  implicit def stringDecoder: Decoder[String] =
    createDecoder[String, JsonToken.Str] { case JsonToken.Str(v) => Right(v) }

  implicit def intDecoder: Decoder[Int] =
    createDecoder[Int, JsonToken.Number[Int]] { case JsonToken.Number(v: Int) =>
      Right(v)
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
    stream =>
  stream.headOption.map {
      case Right(JsonToken.Null -> _) =>
        Right(Option.empty[A] -> stream.tail)
      case _ =>
        aDecoder.decode(stream).map { case (value, tail) => Some(value) -> tail }
    }.getOrElse(Left(StreamingDecodingFailure("Empty stream")))


  // implicit def listDecoder[A](implicit aDecoder: Decoder[A]): Decoder[List[A]] =
  //   withExpected[List[A], JsonToken.BeginArray](_) { case ((_: JsonToken.BeginArray, _), tail) =>
  //     @tailrec
  //     def decodeNext(stream: TokenStream, result: List[A]): StreamingDecoderResult[List[A]] =
  //       stream
  //         .headOption
  //         .fold[Either[StreamingDecoderResult[List[A]], StreamingDecoderResult[List[A]]]](Left(endOfDocumentError[List[A], JsonToken.EndObject])) {
  //           _.fold(
  //             err => Left(Left(StreamingDecodingFailure(err.toString()))),
  //             {
  //               case JsonToken.EndArray -> _ => Left(Right(result -> stream.tail))
  //               case _                       =>
  //                 Right(aDecoder.decode(stream).map{ case value -> remaining => List(value) -> remaining})
  //             }
  //           )
  //         } match {
  //           case Right(Right(value -> tail)) => decodeNext(tail, value ++ result)
  //           case Right(other) => other
  //           case Left(other) => other
  //         }

  //     decodeNext(tail, Nil).map { case (list, tail) =>
  //         list.reverse -> tail
  //     }
  //   }
  implicit def listDecoder[A](implicit aDecoder: Decoder[A]): Decoder[List[A]] =
    withExpected[List[A], JsonToken.BeginArray](_) { case ((_: JsonToken.BeginArray, _), tail) =>
      def decodeNext(stream: TokenStream, result: List[A]): Eval[StreamingDecoderResult[List[A]]] =
        stream.headOption.fold(Eval.now(endOfDocumentError[List[A], JsonToken.EndObject])) {
          _.fold(
            err => Eval.now(Left(StreamingDecodingFailure(err.toString()))),
            {
              case JsonToken.EndArray -> _ => Eval.now(Right(result -> stream.tail))
              case _                       =>
                Eval.later(aDecoder.decode(stream)).flatMap {
                  _.fold(
                    err => Eval.now(Left(err)),
                    { case value -> tail => decodeNext(tail, value :: result) }
                  )
                }
            }
          )
        }

      decodeNext(tail, Nil).map {
        _.map { case (list, tail) =>
          list.reverse -> tail
        }
      }.value
    }

  // implicit def mapDecoder[V](implicit valueDecoder: Decoder[V]): Decoder[Map[String, V]] =
  //   stream =>
  //     withExpected[Map[String, V], JsonToken.BeginObject](stream) { case ((_: JsonToken.BeginObject, _) -> tail) =>
  //       @tailrec
  //       def decodeNext(stream: TokenStream, result: Map[String, V]): StreamingDecoderResult[Map[String, V]] =
  //         stream
  //           .headOption
  //           .fold[Either[StreamingDecoderResult[Map[String, V]], StreamingDecoderResult[Map[String, V]]]](Left(Left(StreamingDecodingFailure("UnexpectedEndOfStream")))) {
  //             _.fold(
  //               err => Left(Left(StreamingDecodingFailure(err.toString))),
  //               {
  //                 case JsonToken.EndObject -> _ => Left(Right(result -> stream.tail))
  //                 case JsonToken.Key(key) -> _  =>
  //                   Right(valueDecoder.decode(stream.tail).map { case (v, r) => Map(key -> v) -> r})
  //                 // _.fold(
  //                 //   err => Left(err),
  //                 //   { case value -> remaining =>
  //                 //     decodeNext(remaining, result + (key -> value))
  //                 //   }
  //                 // )
  //                 case token -> pos             =>
  //                     Left(
  //                       Left(
  //                         StreamingDecodingFailure(
  //                           s"Expected ${TokenName[JsonToken.Key]
  //                             .show()} or ${TokenName[JsonToken.EndObject]} but got ${TokenName(token).show()} at $pos"
  //                         )
  //                       )
  //                     )

  //               }
  //           )
  //           }
  //          match {
  //             case Right(Right(m -> remaining)) => decodeNext(remaining, result ++ m)
  //             case Right(other) => other
  //             case Left(other) => other
  //           }
      

  //       decodeNext(tail, Map.empty)
  //     }

  implicit def mapDecoder[V](implicit valueDecoder: Decoder[V]): Decoder[Map[String, V]] =
    stream =>
      withExpected[Map[String, V], JsonToken.BeginObject](stream) { case ((_: JsonToken.BeginObject, _) -> tail) =>
        def decodeNext(stream: TokenStream, result: Map[String, V]): Eval[StreamingDecoderResult[Map[String, V]]] =
          stream.headOption
            .fold(Eval.now[StreamingDecoderResult[Map[String, V]]](Left(StreamingDecodingFailure("UnexpectedEndOfStream")))) {
              _.fold(
                e => Eval.now(Left(StreamingDecodingFailure(e.toString()))),
                {
            case JsonToken.EndObject -> _ => Eval.now(Right(result -> stream.tail))
            case JsonToken.Key(key) -> _  =>
              Eval.later(valueDecoder.decode(stream.tail)).flatMap {
                _.fold(
                  err => Eval.now(Left(err)),
                  { case value -> remaining =>
                    decodeNext(remaining, result + (key -> value))
                  }
                )
              }
            case token -> pos             =>
              Eval.now(
                Left(
                  StreamingDecodingFailure(
                    s"Expected ${TokenName[JsonToken.Key]
                      .show()} or ${TokenName[JsonToken.EndObject]} but got ${TokenName(token).show()} at $pos"
                  )
                )
              )

                }
              )
          }

        decodeNext(tail, Map.empty).value
      }

  private def createDecoder[A, Token <: JsonToken: ClassTag: TokenName](
    decodeToken: Token => Either[StreamingDecoderError, A]
  ): Decoder[A] =
    new Decoder[A] {
      def decode(stream: TokenStream): StreamingDecoderResult[A] =
        stream.headOption.fold[StreamingDecoderResult[A]](
          Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got 'END DOCUMENT'"))
        ) {
          _.fold(
            err => Left(StreamingDecodingFailure(err.toString)),
            {
              case (token: Token) -> _ => decodeToken(token).map(_ -> stream.tail)
              case token -> pos        =>
                Left(
                  StreamingDecodingFailure(
                    s"Expected ${TokenName[Token].show()}, but got ${TokenName(token).show()} at $pos"
                  )
                )
            }
          )
        }
    }

  private[ghyll] def withExpected[A, Token <: JsonToken: TokenName](
    stream: TokenStream
  )(
    pf: PartialFunction[((JsonToken, List[Pos]), TokenStream), StreamingDecoderResult[A]]
  ): StreamingDecoderResult[A] =
    stream.headOption.fold(endOfDocumentError[A, Token]) {
      _.fold[StreamingDecoderResult[A]](
        err => Left(StreamingDecodingFailure(err.toString())),
        head => pf.orElse(expected[A, Token])(head -> stream.tail)
      )
    }

  // stream match {
  //   case Right(head) #:: tail =>
  //     pf.orElse(expected[A, Token])(head -> tail)
  //   case LazyList() =>
  //     endOfDocumentError[A, Token]
  // }

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
