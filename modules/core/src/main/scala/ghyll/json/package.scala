package ghyll

package object json {
  type ReadResult = Either[TokeniserError, (List[Pos], JsonToken)]
}
