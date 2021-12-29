package ghyll.json

import ghyll.json.JsonTokenReader.JsonTokenReaderResult

trait TestJsonTokenWriter[F[_]] extends JsonTokenWriter[F] {
  def written: F[List[JsonTokenReaderResult]]
}

object TestJsonTokenWriter {
  def apply[F[_]]: TestJsonTokenWriter[F] = ???
}


