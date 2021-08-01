package ghyll

import cats.effect.IO
import ghyll.auto.{DerivedDecoder, DerivedEncoder}
import ghyll.{StreamingDecoderResult, StreamingEncoderResult}

abstract class  DecoderIO[A] extends Decoder[IO, A]

object DecoderIO:
  inline given derived[A](using d: DerivedDecoder[IO, A]): DecoderIO[A] = 
    new DecoderIO[A] {
        def decode(stream: TokenStream[IO]): StreamingDecoderResult[IO, A] =
          d.decode(stream)
    }

abstract class  EncoderIO[A] extends Encoder[IO, A]

object EncoderIO:
  inline given derived[A](using e: DerivedEncoder[IO, A]): EncoderIO[A] = 
    new EncoderIO[A] {
        def encode(value: A): StreamingEncoderResult[IO] =
          e.encode(value)
    }

abstract class CodecIO[A] extends Codec[IO, A]

object CodecIO:
  inline given derived[A](using d: DerivedDecoder[IO, A], e: DerivedEncoder[IO, A]): CodecIO[A] = 
    new CodecIO[A] {
        def decode(stream: TokenStream[IO]): StreamingDecoderResult[IO, A] =
          d.decode(stream)

        def encode(value: A): StreamingEncoderResult[IO] =
          e.encode(value)

    }
