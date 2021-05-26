package ghyll

sealed trait Pos

@SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
object Pos {
  case object Obj extends Pos
  case object Array extends Pos
  case class ObjectKey(name: String) extends Pos
  case class ArrayIndex(index: Int) extends Pos

  def nextPos(pos: List[Pos]): List[Pos] =
    pos match {
      case Pos.ArrayIndex(n) :: tail => Pos.ArrayIndex(n + 1) :: tail
      case Pos.ObjectKey(_) :: tail  => tail
      case _                         => pos
    }

  def nextKey(pos: List[Pos], name: String): List[Pos] =
    pos match {
      case Pos.ObjectKey(_) :: tail => Pos.ObjectKey(name) :: tail
      case _                        => Pos.ObjectKey(name) :: pos
    }

  def endArray(pos: List[Pos]): List[Pos] =
    pos match {
      case Pos.ArrayIndex(_) :: Pos.Array :: tail => tail
      case Pos.Array :: tail                      => tail
      case _                                      => throw new IllegalStateException("this shouldn't happen")
    }

  def endObj(pos: List[Pos]): List[Pos] =
    pos match {
      case Pos.ObjectKey(_) :: Pos.Obj :: tail => tail
      case Pos.Obj :: tail                     => tail
      case _                                   => throw new IllegalStateException("this shouldn't happen")
    }

}
