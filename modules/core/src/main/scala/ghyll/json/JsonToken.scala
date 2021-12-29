package ghyll.json

import cats.Eq

sealed trait JsonToken

@SuppressWarnings(Array("scalafix:DisableSyntax.throw")) // fixme!
object JsonToken {
  implicit val eq: Eq[JsonToken] = Eq.fromUniversalEquals

  type BeginObject = BeginObject.type
  type EndObject = EndObject.type
  type BeginArray = BeginArray.type
  type EndArray = EndArray.type
  type Key = Key.type
  type Str = Str.type
  type Number = Number.type
  type Boolean = Boolean.type
  type Null = Null.type

  case object BeginObject extends JsonToken
  case object EndObject extends JsonToken
  case object BeginArray extends JsonToken
  case object EndArray extends JsonToken

  case object Key extends JsonToken
  case object Str extends JsonToken
  case object Number extends JsonToken

  case object Boolean extends JsonToken
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
        case BeginObject        => beginObject
        case EndObject          => endObject
        case BeginArray         => beginArray
        case EndArray           => endArray
        case Key                => key
        case Str                => str
        case Number             => number
        case Boolean            => boolean
        case Null               => nulll
      }
  }
}
