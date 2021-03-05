package ghyll.derivation

import ghyll.Decoder
import ghyll.derivation.FieldDecoder.Field

// base idea from: https://stackoverflow.com/a/53438635
private[ghyll] trait FieldDecoder[T] {
  def fields: List[Field]
}

private[ghyll] object FieldDecoder extends FieldDecoderInstances {
  trait Field {
    type Out
    def name: String
    def decoder: Decoder[Out]
  }
}
