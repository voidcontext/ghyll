package com.gaborpihaj.jsonstream

package object v2 {
  type StreamingDecoderResult[A] = Either[StreamingDecoderError, A]
}
