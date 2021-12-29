package ghyll.json

sealed trait JsonValue

object JsonValue {
  case class Key(name: String) extends JsonValue
  case class Str(value: String) extends JsonValue
  case class Number[Repr](value: Repr) extends JsonValue

  case class Boolean(value: scala.Boolean) extends JsonValue
  case object Null extends JsonValue

}
