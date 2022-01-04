package ghyll.json

import cats.Eq
import ghyll.Pos

sealed trait JsonToken

@SuppressWarnings(Array("scalafix:DisableSyntax.throw")) // fixme!
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
  case class Number[Repr](n: Repr) extends JsonToken

  case class Boolean(value: scala.Boolean) extends JsonToken
  case object Null extends JsonToken

  trait TokenName[+T <: JsonToken] {
    def show(): String
  }

  implicit class JsonTokenOps[T <: JsonToken](t: T) {
    def widen: JsonToken = t
  }

  // TODO: test this
  def pos(token: JsonToken, stack: List[Pos]): List[Pos] =
    token match {
      case BeginArray                             => Pos.ArrayIndex(0) :: Pos.Array :: stack
      case EndArray                               => Pos.endArray(stack)
      case BeginObject                            => Pos.Obj :: stack
      case EndObject                              => Pos.endObj(stack)
      case Number(_) | Str(_) | Boolean(_) | Null => Pos.nextPos(stack)
      case Key(n)                                 => Pos.nextKey(stack, n)
    }

  object TokenName {
    implicit val beginObject: TokenName[BeginObject] = () => "BeginObject"
    implicit val endObject: TokenName[EndObject] = () => "EndObject"
    implicit val beginArray: TokenName[BeginArray] = () => "BeginArray"
    implicit val endArray: TokenName[EndArray] = () => "EndArray"
    implicit val key: TokenName[Key] = () => "Key"
    implicit val str: TokenName[Str] = () => "Str"
    implicit val numberString: TokenName[Number[String]] = () => "Number[String]"
    implicit val numberInt: TokenName[Number[Int]] = () => "Number[Int]"
    implicit val numberBigDecimal: TokenName[Number[BigDecimal]] = () => "Number[BigDecimal]"
    implicit val boolean: TokenName[Boolean] = () => "Boolean"
    implicit val nulll: TokenName[Null] = () => "Null"

    def apply[T <: JsonToken](implicit ev: TokenName[T]): TokenName[T] = ev
    def apply(token: JsonToken): TokenName[JsonToken] =
      token match {
        case BeginObject           => beginObject
        case EndObject             => endObject
        case BeginArray            => beginArray
        case EndArray              => endArray
        case _: Key                => key
        case _: Str                => str
        case Number(_: String)     => numberString
        case Number(_: Int)        => numberInt
        case Number(_: BigDecimal) => numberBigDecimal
        case Number(_)             => throw new Exception("unimplemented")
        case _: Boolean            => boolean
        case Null                  => nulll
      }
  }
}
