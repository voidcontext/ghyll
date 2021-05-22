package ghyll.auto

import ghyll.{StreamingDecoderError, StreamingDecodingFailure}
import scala.compiletime.{constValue, erasedValue, summonInline}

trait ReprMapper[T]:
  def fromMap(map: Map[String, Any], fieldNames: List[String]): Either[StreamingDecoderError, T]


object ReprMapper:
  given emptyTupleReprMapper: ReprMapper[EmptyTuple] =
    new ReprMapper[EmptyTuple]:
      def fromMap(map: Map[String, Any], fieldNames: List[String]): Either[StreamingDecoderError, EmptyTuple] =
        Right(EmptyTuple)

  given tupleReprMapper[A, T <: Tuple](using tailMapper: ReprMapper[T]): ReprMapper[A *: T] =
    new ReprMapper[A *: T]:
      def fromMap(map: Map[String, Any], fieldNames: List[String]): Either[StreamingDecoderError, A *: T] =
        map.get(fieldNames.head)
          .toRight(StreamingDecodingFailure(s"Couldn't find decoded value of ${fieldNames.head}"))
          .flatMap { decoded =>
            tailMapper.fromMap(map, fieldNames.tail).map { tail =>
              decoded.asInstanceOf[A] *: tail
            }
          }

  given tupleOptionReprMapper[A, T <: Tuple](using tailMapper: ReprMapper[T]): ReprMapper[Option[A] *: T] =
    new ReprMapper[Option[A] *: T]:
      def fromMap(map: Map[String, Any], fieldNames: List[String]): Either[StreamingDecoderError, Option[A] *: T] =
        tailMapper.fromMap(map, fieldNames.tail).map { tail =>
           map.get(fieldNames.head).getOrElse(None).asInstanceOf[Option[A]] *: tail
        }

