package ghyll.derivation

import ghyll.{StreamingDecoderError, StreamingDecoderResult, StreamingDecodingFailure}
import shapeless._
import shapeless.labelled.{FieldType, field}

trait ReprMapper[A] {
  def fromMap(map: Map[String, Any]): Either[StreamingDecoderError, A]
}

@SuppressWarnings(Array("scalafix:DisableSyntax.asInstanceOf"))
object ReprMapper {
  implicit def hnilDFM: ReprMapper[HNil] =
    new ReprMapper[HNil] {
      def fromMap(map: Map[String, Any]): Either[StreamingDecoderError, HNil] =
        Right(HNil)
    }

  implicit def hconsDecodeFromMap[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    ev: LowPriority,
    t: Lazy[ReprMapper[T]]
  ): ReprMapper[FieldType[K, H] :: T] = {
    val (_) = (ev)
    new ReprMapper[FieldType[K, H] :: T] {
      def fromMap(map: Map[String, Any]): StreamingDecoderResult[FieldType[K, H] :: T] =
        map
          .get(witness.value.name)
          .toRight(StreamingDecodingFailure(s"Couldn't find decoded value of ${witness.value.name}"))
          .flatMap { decoded =>
            decodeTail(decoded, t.value, map)
          }
    }
  }

  implicit def hconsOptionDecodeFromMap[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    t: Lazy[ReprMapper[T]]
  ): ReprMapper[FieldType[K, Option[H]] :: T] =
    new ReprMapper[FieldType[K, Option[H]] :: T] {
      def fromMap(map: Map[String, Any]): StreamingDecoderResult[FieldType[K, Option[H]] :: T] =
        map
          .get(witness.value.name)
          .fold(decodeTail(None, t.value, map)) { decoded =>
            decodeTail(decoded, t.value, map)
          }
    }

  @inline private def decodeTail[K <: Symbol, H, T <: HList](
    decoded: Any,
    t: ReprMapper[T],
    map: Map[String, Any]
  ): StreamingDecoderResult[FieldType[K, H] :: T] =
    t
      .fromMap(map)
      .map(tail => field[K](decoded.asInstanceOf[H]) :: tail) // TODO: Find a way to get rid of `asInstanceOf`

}
