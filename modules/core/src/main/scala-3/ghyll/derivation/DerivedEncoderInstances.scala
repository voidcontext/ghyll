package ghyll.derivation

import scala.deriving.Mirror
import ghyll.{Encoder, StreamingEncodingFailure, StreamingEncoderResult}
import ghyll.StreamingEncoderResult.catchEncodingFailure
import com.google.gson.stream.JsonWriter
import cats.syntax.flatMap._
import cats.instances.either._

import scala.compiletime.{erasedValue, summonInline}


trait DerivedEncoderInstances:
  inline def summonInstances[T <: Tuple]: List[Encoder[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[Encoder[t]] :: summonInstances[ts]

  inline given derived[A](using m: Mirror.Of[A]): DerivedEncoder[A] =
        new DerivedEncoder[A]:
          def encode(writer: JsonWriter, value: A): StreamingEncoderResult =
            inline m match
              case s: Mirror.ProductOf[A] =>
                val elemInstances = summonInstances[s.MirroredElemTypes]

                catchEncodingFailure(writer.beginObject) >>
                  getElemLabels[s.MirroredElemLabels]
                    .zip(elemInstances)
                    .zip(value.asInstanceOf[Product].productIterator)
                    .foldLeft[StreamingEncoderResult](Right(())) { case (acc, ((label, encoder), vvalue)) =>
                      acc >> catchEncodingFailure(writer.name(label)) >> encoder.encode(writer, vvalue.asInstanceOf[encoder.For])
                    } >>
                      catchEncodingFailure(writer.endObject)

              case _ => Left(StreamingEncodingFailure("unimplemented"))

