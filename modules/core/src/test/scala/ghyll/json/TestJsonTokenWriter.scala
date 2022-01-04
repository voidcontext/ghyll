package ghyll.json

trait TestJsonTokenWriter[F[_]] extends JsonTokenWriter[F] {
  def written: F[List[JsonToken]]
}

object TestJsonTokenWriter {
  def apply[F[_]]: TestJsonTokenWriter[F] = ???
}
