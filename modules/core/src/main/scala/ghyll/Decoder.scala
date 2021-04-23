package ghyll

import java.time.LocalDate

import scala.annotation.tailrec

import cats.instances.either._
import cats.syntax.eq._
import cats.syntax.flatMap._
import com.google.gson.stream.{JsonReader, JsonToken}
import ghyll.StreamingDecoderResult.catchDecodingFailure
import ghyll.gson.Implicits._

trait Decoder[A] {
  def decode(reader: JsonReader): StreamingDecoderResult[A]

  def map[B](f: A => B): Decoder[B] = reader => decode(reader).map(f)
}

object Decoder {
  def apply[A](implicit ev: Decoder[A]) = ev

  implicit val stringDecoder: Decoder[String] =
    createDecoder[String](JsonToken.STRING)(_.nextString())

  implicit val intDecoder: Decoder[Int] =
    createDecoder[Int](JsonToken.NUMBER)(_.nextInt())

  implicit val booleanDecoder: Decoder[Boolean] =
    createDecoder[Boolean](JsonToken.BOOLEAN)(_.nextBoolean())

  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    createDecoder[BigDecimal](JsonToken.NUMBER)(r => BigDecimal(r.nextString()))

  implicit val localDateDecoder: Decoder[LocalDate] =
    createDecoder[LocalDate](JsonToken.STRING)(r => LocalDate.parse(r.nextString()))

  implicit def optionDecoder[A](implicit aDecoder: Decoder[A]): Decoder[Option[A]] =
    reader =>
      if (reader.peek() === JsonToken.NULL) { reader.nextNull(); Right(None) }
      else aDecoder.decode(reader).map(Option(_))

  implicit def listDecoder[A](implicit aDecoder: Decoder[A]): Decoder[List[A]] =
    reader => {
      @tailrec
      def decodeItems(result: StreamingDecoderResult[List[A]]): StreamingDecoderResult[List[A]] =
        if (reader.peek() === JsonToken.END_ARRAY || result.isLeft) result
        // In the following expression we prepend all newly decoded
        // element to the list of already decoded items. With this
        // the list will be reversed, but from a performance
        // perspective it's still better to prepend then reverse at
        // the and than appending to the list
        else decodeItems(result.flatMap(list => aDecoder.decode(reader).map(_ :: list)))

      withToken(reader, JsonToken.BEGIN_ARRAY)(_.beginArray()) >>
        // To preserve the orignal order we need to reverse the list
        // here
        decodeItems(Right(List.empty))
          .map(_.reverse)
          .flatTap { _ =>
            reader.endArray()
            Right(())
          }
    }

  implicit def mapDecoder[V](implicit valueDecoder: Decoder[V]): Decoder[Map[String, V]] =
    reader => {
      @tailrec
      def decodeKeyValues(result: StreamingDecoderResult[Map[String, V]]): StreamingDecoderResult[Map[String, V]] =
        if (reader.peek() === JsonToken.END_OBJECT || result.isLeft) result
        else {
          val key = reader.nextName()
          decodeKeyValues(
            result.flatMap(m => valueDecoder.decode(reader).map(decoded => m + (key -> decoded)))
          )
        }

      withToken(reader, JsonToken.BEGIN_OBJECT)(_.beginObject()) >>
        decodeKeyValues(Right(Map.empty)).flatTap { _ =>
          reader.endObject()
          Right(())
        }
    }

  private def withToken[A](reader: JsonReader, token: JsonToken)(f: JsonReader => A): StreamingDecoderResult[A] =
    if (reader.peek() === token) catchDecodingFailure(f(reader))
    else Left(StreamingDecodingFailure(s"Expected ${token}, but got ${reader.peek()}"))

  private def createDecoder[A](token: JsonToken)(decode: JsonReader => A): Decoder[A] =
    reader => withToken(reader, token)(decode)

}
