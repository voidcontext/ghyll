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

  implicit def optionDecoder[A]/* (implicit aDecoder: Decoder[A]) */ : Decoder[Option[A]] =
    _ match {
      case Right(JsonToken.Null -> _) #:: tail =>
        Right(Option.empty[A] -> tail)
      // TODO
      case _ => Left(StreamingDecodingFailure("Unimplemented"))
    }



    // _.pull.uncons1
    //   .flatMap[F, Either[StreamingDecoderError, (Option[A], TokenStream)], Unit] {
    //     case Some((JsonToken.Null, _) -> tail) => Pull.output1(Right(Option.empty[A] -> tail))
    //     case Some(tokenWithPos -> tail)        =>
    //       aDecoder.decode(tail.cons1(tokenWithPos)).map(_.map { case (a, s) => Some(a) -> s }).pull.echo
    //     case None                              => Pull.output1(Left(StreamingDecodingFailure(s"Expected optional value but got 'EndOfDocument'")))
    //   }
    //   .stream

  implicit def listDecoder[A] /*(implicit aDecoder: Decoder[F, A])*/ : Decoder[List[A]] =
    _ => Left(StreamingDecodingFailure("Unimplemented"))
      // withExpected[F, List[A], JsonToken.BeginArray](stream) { case ((_: JsonToken.BeginArray, _), tail) =>
      //   def decodeNext(
      //     stream: TokenStream[F],
      //     result: List[A]
      //   ): Pull[F, Either[StreamingDecoderError, (List[A], TokenStream[F])], Unit] =
      //     stream.pull.uncons1.flatMap {
      //       case Some((JsonToken.EndArray, _) -> tail) => Pull.output1(Right(result.reverse -> tail))
      //       case Some(tokenWithPos -> tail)            =>
      //         aDecoder.decode(tail.cons1(tokenWithPos)).pull.uncons1.flatMap {
      //           case Some(Right(value -> tail) -> _) => decodeNext(tail, value :: result)
      //           case Some(Left(err) -> _)            => Pull.output1(Left(err))
      //           case None                            => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
      //         }
      //       case None                                  => Pull.output1(Left(StreamingDecodingFailure("Expected list item, but got 'EndOfDocument'")))
      //     }

      //   decodeNext(tail, List.empty).stream
      // }

  implicit def mapDecoder[V] /*(implicit valueDecoder: Decoder[F, V])*/ : Decoder[Map[String, V]] =
    _ => Left(StreamingDecodingFailure("Unimplemented"))
    // stream =>
    //   withExpected[F, Map[String, V], JsonToken.BeginObject](stream) { case ((_: JsonToken.BeginObject, _), tail) =>
    //     def decodeNext(
    //       stream: TokenStream[F],
    //       result: Map[String, V]
    //     ): Pull[F, Either[StreamingDecoderError, (Map[String, V], TokenStream[F])], Unit] =
    //       stream.pull.uncons1.flatMap {
    //         case Some((JsonToken.EndObject, _) -> tail) => Pull.output1((Right(result -> tail)))
    //         case Some((JsonToken.Key(name), _) -> tail) =>
    //           valueDecoder.decode(tail).pull.uncons1.flatMap {
    //             case Some(Right(value -> tail) -> _) => decodeNext(tail, result + (name -> value))
    //             case Some(Left(err) -> _)            => Pull.output1(Left(err))
    //             case None                            => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
    //           }
    //         case Some((token, pos) -> _)                =>
    //           Pull.output1(
    //             Left(
    //               StreamingDecodingFailure(
    //                 s"Expected ${TokenName[JsonToken.Key].show()} but got ${TokenName(token).show()} at $pos"
    //               )
    //             )
    //           )
    //         case None                                   =>
    //           Pull.output1(
    //             Left(StreamingDecodingFailure(s"Expected ${TokenName[JsonToken.Key].show()}, but got 'EndOfDocument'"))
    //           )
    //       }

    //     decodeNext(tail, Map.empty).stream
    //   }

  private def createDecoder[A, Token <: JsonToken: ClassTag: TokenName](
    decodeToken: Token => Either[StreamingDecoderError, A]
  ): Decoder[A] =
    new Decoder[A] {
      def decode(stream: TokenStream): StreamingDecoderResult[A] =
        stream match {
          case Right((token: Token) -> _) #:: tail =>
            decodeToken(token).map(_ -> tail)
          case Right(_ -> pos) #:: _ =>
            Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got ${TokenName[Token].show()} at $pos"))
          case Left(err) #:: _ =>
            Left(StreamingDecodingFailure(err.toString))
          case LazyList() =>
            Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got 'END DOCUMENT'"))
        }

    }



  //   withExpected[A, Token](_) { case ((token: Token, _), stream) =>
  //     Stream.emit(pf(token).map(_ -> stream)).covary[F]
  //   }

  // private[ghyll] def withExpected[A, Token <: JsonToken: TokenName](
  //   stream: TokenStream
  // )(
  //   pf: PartialFunction[((JsonToken, List[Pos]), TokenStream), StreamingDecoderResult[A]]
  // ): StreamingDecoderResult[A] =
  //   stream match {
  //     case head #:: tail =>
  //       pf.orElse(expected[A, Token])(head -> tail)
  //   }

  //   stream.pull.uncons1
  //     .flatMap[F, Either[StreamingDecoderError, (A, TokenStream[F])], Unit] {
  //       case Some(tokenWithPos -> tail) =>
  //         pf.orElse(expected[A, Token])(tokenWithPos -> tail).pull.echo
  //       case None                       =>
  //         Pull.output1(Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()} but got 'EndOfDocument'")))
  //     }
  //     .stream

  // private def expected[A, Token <: JsonToken: TokenName]
  //   : PartialFunction[((JsonToken, List[Pos]), TokenStream), StreamingDecoderResult[A]] = { case ((t, pos), _) =>
  //   LazyList(
  //     Left(StreamingDecodingFailure(s"Expected ${TokenName[Token].show()}, but got ${TokenName(t).show()} at $pos"))
  //   )
  // }

  // TODO: make sure pos is accessible here
  private def catchDecodingFailure[A](body: => A): Either[StreamingDecoderError, A] =
    Either.catchNonFatal(body).left.map[StreamingDecoderError](t => StreamingDecodingFailure(t.getMessage()))

}
