package ghyll.auto

import cats.syntax.eq._
import ghyll.json.JsonToken._
import ghyll.{Decoder, StreamingDecoderResult, StreamingDecodingFailure, TokenStream}
import shapeless.LabelledGeneric

private[ghyll] trait DerivedDecoderInstances {
  implicit def derivedDecoderGeneric[A, H](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[A],
    mapper: ReprMapper[H]
  ): DerivedDecoder[A] =
    new DerivedDecoder[A] {
      def decode(stream: TokenStream): StreamingDecoderResult[A] =
        Decoder.withExpected[A, BeginObject](stream) { case ((_: BeginObject, _), tail) =>
          decodeKeys(tail, Map.empty, fieldDecoders)
            .flatMap { case m -> remaining =>
              mapper.fromMap(m).map(lg.from(_) -> remaining)
            }
        }
    }

  private[this] def decodeKeys[A](
    stream: TokenStream,
    acc: Map[String, Any],
    fieldDecoders: FieldDecoder[A]
  ): StreamingDecoderResult[Map[String, Any]] =
    stream.headOption.map {
      _.fold(
        _ => Left(StreamingDecodingFailure("Token stream error")),
        {
          case EndObject -> _ => Right(acc -> stream.tail)
          case Key(key) -> _  =>
            fieldDecoders.fields.find(_.name === key) match {
              case None        => decodeKeys(TokenStream.skipValue(stream.tail), acc, fieldDecoders)
              case Some(field) =>
                field.decoder.decode(stream.tail)
                  .flatMap { case value -> remaining =>
                    decodeKeys(remaining, acc + (key -> value), fieldDecoders)
                  }
            }
          case _              =>
            Left(StreamingDecodingFailure("Unexpected token"))
        }
      )
    }.getOrElse(Left(StreamingDecodingFailure("Unexpected end of token stream")))
}
