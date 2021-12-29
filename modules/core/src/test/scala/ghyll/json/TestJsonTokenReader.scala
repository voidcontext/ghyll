package ghyll.json

import ghyll.json.JsonTokenReader.JsonTokenReaderResult

trait TestJsonTokenReader[F[_]] extends JsonTokenReader[F]

object TestJsonTokenReader {
  def withTokens[F[_]](tokens: List[JsonTokenReaderResult]): TestJsonTokenReader[F] = ???
}
