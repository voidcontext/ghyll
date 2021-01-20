package com.gaborpihaj.jsonstream.v2

sealed trait StreamingDecoderError
case object Unimplemented extends StreamingDecoderError
case class StreamingDecodingFailure(message: String) extends StreamingDecoderError
