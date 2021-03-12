package ghyll

import java.io.{ByteArrayInputStream, InputStreamReader, OutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import cats.effect.{IO, Resource}
import com.google.gson.stream.{JsonReader, JsonWriter}

object Utils {
  def createReader(json: String): Resource[IO, JsonReader] =
    Resource.fromAutoCloseable(
      IO.delay {
        new JsonReader(new InputStreamReader(new ByteArrayInputStream(json.getBytes())))
      }
    )

  def createWriter(out: Resource[IO, OutputStream]): Resource[IO, JsonWriter] =
    out.flatMap { outStream =>
      Resource.fromAutoCloseable(
        IO.delay(
          new JsonWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_16))
        )
      )
    }
}
