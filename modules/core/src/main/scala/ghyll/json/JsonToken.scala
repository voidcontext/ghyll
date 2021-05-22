package ghyll.json

import cats.Eq

sealed trait JsonToken

object JsonToken {
  implicit val eq: Eq[JsonToken] = Eq.fromUniversalEquals

  type BeginObject = BeginObject.type
  type EndObject = EndObject.type
  type BeginArray = BeginArray.type
  type EndArray = EndArray.type
  type Null = Null.type

  case object BeginObject extends JsonToken
  case object EndObject extends JsonToken
  case object BeginArray extends JsonToken
  case object EndArray extends JsonToken

  case class Key(name: String) extends JsonToken
  case class Str(value: String) extends JsonToken
  case class Number(value: String) extends JsonToken
  case class Boolean(value: scala.Boolean) extends JsonToken
  case object Null extends JsonToken

  trait TokenName[+T <: JsonToken] {
    def show(): String
  }

  object TokenName {
    implicit val beginObject: TokenName[BeginObject] = () => "BeginObject"
    implicit val endObject: TokenName[EndObject] = () => "EndObject"
    implicit val beginArray: TokenName[BeginArray] = () => "BeginArray"
    implicit val endArray: TokenName[EndArray] = () => "EndArray"
    implicit val key: TokenName[Key] = () => "Key"
    implicit val str: TokenName[Str] = () => "Str"
    implicit val number: TokenName[Number] = () => "Number"
    implicit val boolean: TokenName[Boolean] = () => "Boolean"
    implicit val nulll: TokenName[Null] = () => "Null"

    def apply[T <: JsonToken](implicit ev: TokenName[T]): TokenName[T] = ev
    def apply(token: JsonToken): TokenName[JsonToken] =
      token match {
        case _: BeginObject => beginObject
        case _: EndObject   => endObject
        case _: BeginArray  => beginArray
        case _: EndArray    => endArray
        case _: Key         => key
        case _: Str         => str
        case _: Number      => number
        case _: Boolean     => boolean
        case _: Null        => nulll
      }
  }
}
