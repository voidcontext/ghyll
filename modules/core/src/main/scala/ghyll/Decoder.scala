package ghyll

import java.time.LocalDate

import scala.annotation.tailrec

import cats.syntax.either._
import com.google.gson.stream.{JsonReader, JsonToken}

trait Decoder[A] {
  def decode(reader: JsonReader): StreamingDecoderResult[A]

  def map[B](f: A => B): Decoder[B] = reader => decode(reader).map(f)
}

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
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
      if (reader.peek() == JsonToken.NULL) { reader.nextNull(); Right(None) }
      else aDecoder.decode(reader).map(Option(_))

  implicit def listDecoder[A](implicit aDecoder: Decoder[A]): Decoder[List[A]] =
    reader => {
      reader.beginArray()

      @tailrec
      def decodeItems(result: StreamingDecoderResult[List[A]]): StreamingDecoderResult[List[A]] =
        if (reader.peek() == JsonToken.END_ARRAY) result
        // In the following expression we prepend all newly decoded
        // element to the list of already decoded items. With this
        // the list will be reversed, but from a performance
        // perspective it's still better to prepend then reverse at
        // the and than appending to the list
        else decodeItems(result.flatMap(list => aDecoder.decode(reader).map(_ :: list)))

      // To preserve the orignal order we need to reverse the list
      // here
      val result = decodeItems(Right(List.empty)).map(_.reverse)
      reader.endArray()
      result
    }

  private def createDecoder[A](token: JsonToken)(decode: JsonReader => A): Decoder[A] =
    reader =>
      if (reader.peek() == token)
        Either
          .catchNonFatal(decode(reader))
          .left
          .map(t => StreamingDecodingFailure(t.getMessage()))
      else Left(StreamingDecodingFailure(s"Expected ${token}, but got ${reader.peek()}"))

}
