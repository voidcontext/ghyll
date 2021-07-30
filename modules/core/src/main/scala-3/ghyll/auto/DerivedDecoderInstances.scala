package ghyll.auto

import scala.annotation.tailrec
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror

import cats.instances.stream
import cats.syntax.eq._
import cats.{Applicative, ApplicativeError}
import fs2.{Stream, Pull}
import ghyll.gson.Implicits._
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName
import ghyll._
import ghyll.TokenStream.skipValue

trait DerivedDecoderInstances:
  trait FieldDecoder[F[_]]:
    type Out
    def d: Decoder[F, Out]

  inline def summonAll[F[_], T <: Tuple]: List[FieldDecoder[F]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) =>
        (new FieldDecoder:
          type Out = t
          val d = summonInline[Decoder[F, t]]) :: summonAll[F, ts]

  inline given derivedDecoderGeneric[F[_], A](using m: Mirror.ProductOf[A], ae: ApplicativeError[F, Throwable]): DerivedDecoder[F, A] =
    new DerivedDecoder[F, A]:
      def decode(stream: TokenStream[F]): StreamingDecoderResult[F, A] =
        lazy val elemLabels = getElemLabels[m.MirroredElemLabels]
        lazy val fieldDecoders =
          elemLabels.zip(summonAll[F, m.MirroredElemTypes]).toMap


        Decoder.withExpected[F, A, JsonToken.BeginObject](stream) { case ((_: JsonToken.BeginObject, _), tail) =>
          decodeKeys[F, A](tail, fieldDecoders).flatMap {
            case Right((map, s)) =>
              Stream.emit(
                summonInline[ReprMapper[m.MirroredElemTypes]]
                  .fromMap(map, elemLabels)
                  .map(m.fromProduct(_) -> s)
              )
            case Left(err) => Stream.emit(Left(err))
          }
        }

  private def decodeKeys[F[_], A](
    stream: TokenStream[F],
    fieldDecoders: Map[String, FieldDecoder[F]]
  )(using ae: ApplicativeError[F, Throwable]): StreamingDecoderResult[F, Map[String, Any]] =
    def decodeNext(s: TokenStream[F], result: Map[String, Any]): Pull[F, Either[StreamingDecoderError, (Map[String, Any], TokenStream[F])], Unit] =
      s.pull.uncons1.flatMap {
        case Some((JsonToken.EndObject, _) -> tail) =>  Pull.output1(Right(result -> tail))
        case Some((JsonToken.Key(name), _) -> tail) =>
          fieldDecoders.get(name).fold(decodeNext(skipValue(tail), result)) { field =>
            field.d.decode(tail).pull.uncons1.flatMap {
              case Some(Right(value -> t) -> _) => decodeNext(t, result + (name -> value))
              case Some(Left(err) -> _) => Pull.output1(Left(err))
              case None                            => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
            }
          }
        case Some((token, pos) -> _)                =>
              Pull.output1(
                Left(
                  StreamingDecodingFailure(
                    s"Expected EndObject or Key but got ${TokenName(token).show()} at $pos"
                  )
                )
              )

       case None                            => Pull.output1(Left(StreamingDecodingFailure("This shouldn't happen")))
      }

    decodeNext(stream, Map.empty).stream

