package ghyll

import java.io.{ByteArrayInputStream, InputStreamReader}

import cats.effect.{IO, Resource}
import com.google.gson.stream.JsonReader

object Utils {
  def createReader(json: String) =
    Resource.fromAutoCloseable(
      IO.delay {
        new JsonReader(new InputStreamReader(new ByteArrayInputStream(json.getBytes())))
      }
    )
}
