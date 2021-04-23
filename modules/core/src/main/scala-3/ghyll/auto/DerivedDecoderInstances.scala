package ghyll.auto

import cats.syntax.eq._
import com.google.gson.stream.{JsonReader, JsonToken}
import ghyll.{Decoder, StreamingDecoderResult, StreamingDecodingFailure}
import ghyll.gson.Implicits._
import scala.annotation.tailrec
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror

trait DerivedDecoderInstances:
  trait FieldDecoder:
    type Out
    def d: Decoder[Out]

  inline def summonAll[T <: Tuple]: List[FieldDecoder] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) =>
        (new FieldDecoder:
          type Out = t
          val d = summonInline[Decoder[t]]) :: summonAll[ts]

  inline given derivedDecoder[A](using m: Mirror.Of[A]): DerivedDecoder[A] =
    new DerivedDecoder[A]:
      def decode(reader: JsonReader): StreamingDecoderResult[A] =
        inline m match
          case s: Mirror.ProductOf[A] =>
            lazy val elemLabels = getElemLabels[m.MirroredElemLabels]
            lazy val fieldDecoders =
              elemLabels.zip(summonAll[s.MirroredElemTypes]).toMap

            reader.beginObject()

            @tailrec
            def decodeKeys(result: StreamingDecoderResult[Map[String, Any]]): StreamingDecoderResult[Map[String, Any]] =
              if (reader.peek() === JsonToken.END_OBJECT) result
              else
                val name = reader.nextName()
                fieldDecoders.get(name) match
                  case None        =>
                    reader.skipValue()
                    decodeKeys(result)
                  case Some(field) =>
                    decodeKeys(
                      for {
                        r       <- result
                        decoded <- field.d.decode(reader)
                      } yield r + (name -> decoded)
                    )

            val decoded = decodeKeys(Right(Map.empty))
            skipRemainingKeys(reader)
            reader.endObject()


            decoded.flatMap { m =>
              summonInline[ReprMapper[s.MirroredElemTypes]]
                .fromMap(m, elemLabels)
                .map(s.fromProduct)
            }
          case _ => Left(StreamingDecodingFailure("error"))

  @SuppressWarnings(Array("scalafix:DisableSyntax.while"))
  @inline private[this] def skipRemainingKeys(reader: JsonReader): Unit =
    while (reader.peek() === JsonToken.NAME) {
      reader.nextName()
      reader.skipValue()
    }
