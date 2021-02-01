package ghyll.jsonpath

sealed trait JsonPath extends Product with Serializable
final case class >::(key: String, tail: JsonPath) extends JsonPath
case object JNil extends JsonPath

object JsonPath {
  implicit class JsonPathOps(path: JsonPath) {
    def >::(parent: String): JsonPath = ghyll.jsonpath.>::(parent, path)
  }
}
