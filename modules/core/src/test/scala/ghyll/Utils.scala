package ghyll

import java.io.{ByteArrayInputStream, InputStreamReader, OutputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets

import cats.effect.{IO, Resource}
import cats.instances.int._
import cats.syntax.eq._
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

  def escape(str: String): String =
    """\p{Cntrl}""".r
      .replaceAllIn(
        str.replaceAll("\\\\", """\\\\"""),
        { m =>
          m.matched
            .codePoints()
            .toArray()
            .toList
            .map {
              case bs if bs === 8  => "\\\\b"
              case bs if bs === 9  => "\\\\t"
              case bs if bs === 10 => "\\\\n"
              case bs if bs === 12 => "\\\\f"
              case bs if bs === 13 => "\\\\r"
              case i if i === 127  => i.toChar
              case i               => "\\\\u%04x".format(i)
            }
            .mkString
        }
      )
      .replaceAll("\"", "\\\\\"")
      .replaceAll("\u2028", "\\\\u2028")
      .replaceAll("\u2029", "\\\\u2029")

}
