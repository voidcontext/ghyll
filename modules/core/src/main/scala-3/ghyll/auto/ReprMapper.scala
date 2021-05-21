package ghyll.auto

// import ghyll.{StreamingDecoderResult, StreamingDecodingFailure}
// import scala.compiletime.{constValue, erasedValue, summonInline}

trait ReprMapper[T] //:
//   def fromMap(map: Map[String, Any], fieldNames: List[String]): StreamingDecoderResult[T]


// object ReprMapper:
//   given emptyTupleReprMapper: ReprMapper[EmptyTuple] =
//     new ReprMapper[EmptyTuple]:
//       def fromMap(map: Map[String, Any], fieldNames: List[String]): StreamingDecoderResult[EmptyTuple] =
//         Right(EmptyTuple)

//   given tupleReprMapper[A, T <: Tuple](using tailMapper: ReprMapper[T]): ReprMapper[A *: T] =
//     new ReprMapper[A *: T]:
//       def fromMap(map: Map[String, Any], fieldNames: List[String]): StreamingDecoderResult[A *: T] =
//         map.get(fieldNames.head)
//           .toRight(StreamingDecodingFailure(s"Couldn't find decoded value of ${fieldNames.head}"))
//           .flatMap { decoded =>
//             tailMapper.fromMap(map, fieldNames.tail).map { tail =>
//               decoded.asInstanceOf[A] *: tail
//             }
//           }

//   given tupleOptionReprMapper[A, T <: Tuple](using tailMapper: ReprMapper[T]): ReprMapper[Option[A] *: T] =
//     new ReprMapper[Option[A] *: T]:
//       def fromMap(map: Map[String, Any], fieldNames: List[String]): StreamingDecoderResult[Option[A] *: T] =
//         tailMapper.fromMap(map, fieldNames.tail).map { tail =>
//            map.get(fieldNames.head).getOrElse(None).asInstanceOf[Option[A]] *: tail
//         }

