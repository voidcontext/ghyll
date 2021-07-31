package ghyll.auto

import scala.deriving.Mirror
import ghyll.{Encoder, StreamingEncodingFailure, StreamingEncoderResult}
import ghyll.json.JsonToken._
import cats.syntax.flatMap._
import cats.instances.either._
import fs2.Stream

import scala.compiletime.{erasedValue, summonInline}


trait DerivedEncoderInstances:
  inline def summonInstances[F[_], T <: Tuple]: List[Encoder[F, _]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) => summonInline[Encoder[F, t]] :: summonInstances[F, ts]

  inline given derivedEncoder[F[_], A](using m: Mirror.ProductOf[A]): DerivedEncoder[F, A] =
        new DerivedEncoder[F, A]:
          def encode(value: A): StreamingEncoderResult[F] =
            val elemInstances = summonInstances[F, m.MirroredElemTypes]

              getElemLabels[m.MirroredElemLabels]
                .zip(elemInstances)
                .zip(value.asInstanceOf[Product].productIterator)
                .foldLeft[StreamingEncoderResult[F]](Right(Stream.emit(BeginObject))) { case (acc, ((label, encoder), vvalue)) =>
                  for {
                    stream <- acc
                    next <- encoder.encode(vvalue.asInstanceOf[encoder.For])
                  } yield stream ++ Stream.emit(Key(label)) ++ next
                }.map (_ ++ Stream.emit(EndObject))
