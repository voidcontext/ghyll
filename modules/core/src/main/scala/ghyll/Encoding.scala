package ghyll

// import java.io.{OutputStream, OutputStreamWriter}

// import cats.effect.{Resource, Sync}
// import com.google.gson.stream.JsonWriter

private[ghyll] trait Encoding {
  // def encode[F[_], A](value: A, outStream: OutputStream)(implicit
  //   encoder: Encoder[A],
  //   F: Sync[F]
  // ): Resource[F, StreamingEncoderResult] =
  //   for {
  //     out    <- Resource.fromAutoCloseable[F, OutputStream](F.delay(outStream))
  //     writer <- Resource.fromAutoCloseable[F, JsonWriter](F.delay(new JsonWriter(new OutputStreamWriter(out))))
  //   } yield encoder.encode(writer, value)

}
