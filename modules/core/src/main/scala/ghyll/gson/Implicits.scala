package ghyll.gson

import cats.kernel.Eq
import com.google.gson.stream.JsonToken

private[ghyll] object Implicits {
  implicit val jsonTokenEq: Eq[JsonToken] = Eq.fromUniversalEquals
}
