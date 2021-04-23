package ghyll.auto

import scala.compiletime.{constValue, erasedValue}

inline def getElemLabels[T <: Tuple]: List[String] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) => (inline constValue[t] match { case str : String => str }) :: getElemLabels[ts]

