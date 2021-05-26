package ghyll.auto

import cats.syntax.eq._
import ghyll.{Decoder, StreamingDecoderResult, StreamingDecodingFailure}
import ghyll.gson.Implicits._
import scala.annotation.tailrec
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror
import ghyll.TokenStream
import ghyll.json.JsonToken
import ghyll.json.JsonToken.TokenName
import cats.ApplicativeError
import fs2.Stream
import cats.instances.stream
import cats.Applicative

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


        Decoder.withExpected[F, A, JsonToken.BeginObject](stream) { case (_: JsonToken.BeginObject, tail) =>
          decodeKeys[F, A](Stream.emit(Right(Map.empty[String, Any] -> tail)), fieldDecoders).flatMap {
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
    stream: StreamingDecoderResult[F, Map[String, Any]],
    fieldDecoders: Map[String, FieldDecoder[F]]
  )(using ae: ApplicativeError[F, Throwable]): StreamingDecoderResult[F, Map[String, Any]] =
    stream.flatMap {
      _ match
        case Right((m, str)) =>
          str.head.flatMap {
            case JsonToken.EndObject => Stream.emit(Right(m -> str.tail))
            case JsonToken.Key(name) =>
              fieldDecoders.get(name) match
                case None        => decodeKeys(Stream.emit(Right(m -> TokenStream.skipValue(str.tail))), fieldDecoders)
                case Some(field) =>
                  decodeKeys(
                    field.d.decode(str.tail).map(_.map { case (a, s) => m + (name -> a) -> s }),
                    fieldDecoders
                  )
            case t                   => Stream.emit(Left(StreamingDecodingFailure(s"Unexpected token: ${TokenName(t).show()}")))
          }
        case e @ Left(_)     => Stream.emit(e)
      
    }

        // reader.beginObject()

  //       @tailrec
  //       def decodeKeys(result: StreamingDecoderResult[F, Map[String, Any]]): StreamingDecoderResult[F, Map[String, Any]] =
  //         if (reader.peek() === JsonToken.END_OBJECT) result
  //         else
  //           val name = reader.nextName()
  //           fieldDecoders.get(name) match
  //             case None        =>
  //               reader.skipValue()
  //               decodeKeys(result)
  //             case Some(field) =>
  //               decodeKeys(
  //                 for {
  //                   r       <- result
  //                   decoded <- field.d.decode(reader)
  //                 } yield r + (name -> decoded)
  //               )

  //       val decoded = decodeKeys(Right(Map.empty))
  //       skipRemainingKeys(reader)
  //       reader.endObject()


  //       decoded.flatMap { map =>
  //         summonInline[ReprMapper[m.MirroredElemTypes]]
  //           .fromMap(map, elemLabels)
  //           .map(m.fromProduct)
  //       }

  // @SuppressWarnings(Array("scalafix:DisableSyntax.while"))
  // @inline private[this] def skipRemainingKeys(reader: JsonReader): Unit =
  //   while (reader.peek() === JsonToken.NAME) {
  //     reader.nextName()
  //     reader.skipValue()
  //   }
