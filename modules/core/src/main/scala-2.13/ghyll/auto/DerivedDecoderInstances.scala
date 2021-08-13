package ghyll.auto

// import cats.ApplicativeError
// import cats.syntax.eq._
// import fs2.Pull
// import fs2.Pull._
// import ghyll.json.JsonToken
// import ghyll.json.JsonToken.TokenName
// import ghyll.{Decoder, StreamingDecoderError, StreamingDecodingFailure, TokenStream}
// import shapeless._
import ghyll.{StreamingDecoderResult, TokenStream}

private[ghyll] trait DerivedDecoderInstances {
  implicit def derivedDecoderGeneric[F[_], A, H] /*(implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[F, A],
    mapper: ReprMapper[H],
    ae: ApplicativeError[F, Throwable]
  )*/ : DerivedDecoder[A] =
    new DerivedDecoder[A] {
      def decode(stream: TokenStream): StreamingDecoderResult[A] = ???
    }
    // stream =>
    //   Decoder.withExpected[F, A, JsonToken.BeginObject](stream) { case ((_: JsonToken.BeginObject, _), tail) =>
    //     decodeKeys(tail, Map.empty, fieldDecoders).stream.pull.uncons1.flatMap {
    //       case Some(Right((m, t)) -> _) =>
    //         Pull.output1(mapper.fromMap(m).map(lg.from(_) -> t))
    //       case Some(Left(err) -> _)     =>
    //         Pull.output1(Left(err))
    //       case None                     => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
    //     }.stream
    //   }

  // private[this] def decodeKeys[F[_], A](
  //   stream: TokenStream[F],
  //   acc: Map[String, Any],
  //   fieldDecoders: FieldDecoder[A]
  // )(implicit
  //   ae: ApplicativeError[F, Throwable]
  // ): Pull[F, Either[StreamingDecoderError, (Map[String, Any], TokenStream)], Unit] =
  //   stream.pull.uncons1.flatMap {
  //     case Some((JsonToken.EndObject, _) -> tail) => Pull.output1(Right(acc -> tail))
  //     case Some((JsonToken.Key(name), _) -> tail) =>
  //       fieldDecoders.fields.find(_.name === name) match {
  //         case None        => decodeKeys(TokenStream.skipValue(tail), acc, fieldDecoders)
  //         case Some(field) =>
  //           field.decoder.decode(tail).pull.uncons1.flatMap {
  //             case Some(Right(value -> tail) -> _) => decodeKeys(tail, acc + (name -> value), fieldDecoders)
  //             case Some(Left(err) -> _)            => Pull.output1(Left(err))
  //             case None                            => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
  //           }
  //       }
  //     case Some((token, pos) -> _)                =>
  //       Pull.output1(
  //         Left(
  //           StreamingDecodingFailure(
  //             s"Expected ${TokenName[JsonToken.Key].show()} but got ${TokenName(token).show()} at $pos"
  //           )
  //         )
  //       )
  //     case None                                   =>
  //       Pull.output1(
  //         Left(StreamingDecodingFailure(s"Expected ${TokenName[JsonToken.Key].show()}, but got 'EndOfDocument'"))
  //       )
  //   }
}
