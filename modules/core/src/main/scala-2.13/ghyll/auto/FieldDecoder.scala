package ghyll.auto

import ghyll.Decoder
import ghyll.auto.FieldDecoder.Field

// base idea from: https://stackoverflow.com/a/53438635
private[ghyll] trait FieldDecoder[F[_], T] {
  def fields: List[Field[F]]
}

private[ghyll] object FieldDecoder extends FieldDecoderInstances {
  trait Field[F[_]] {
    type Out
    def name: String
    def decoder: Decoder[F, Out]
  }
}
