package com.gaborpihaj.jsonstream

import java.io.{InputStream, InputStreamReader}
import java.time.LocalDate

import scala.annotation.tailrec

import cats.effect.{Resource, Sync}
import cats.syntax.either._
import com.google.gson.stream.{JsonReader, JsonToken}
import fs2.Stream
import shapeless._
import shapeless.labelled.FieldType

@SuppressWarnings(
  Array("scalafix:DisableSyntax.==", "scalafix:DisableSyntax.asInstanceOf", "scalafix:DisableSyntax.while")
)
object StreamingDecoder2 {

  sealed trait StreamingDecoderError
  case object Unimplemented extends StreamingDecoderError
  case class StreamingDecodingFailure(message: String) extends StreamingDecoderError

  type StreamingDecoderResult[A] = Either[StreamingDecoderError, A]

  trait StreamingDecoder[F[_]] {
    def decodeArray[T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[T]]]
    def decodeObject[T: Decoder](jaon: InputStream): Resource[F, StreamingDecoderResult[T]]
    def decodeKeyValues[T: Decoder](
      json: InputStream
    ): Resource[F, Stream[F, StreamingDecoderResult[
      (String, T)
    ]]] // TODO: make this more generic: StreamingDecoderResult[(K, V)]
  }

  def decoder[F[_]: Sync]: StreamingDecoder[F] =
    new StreamingDecoder[F] {

      def decodeArray[T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[T]]] = ???

      def decodeObject[T: Decoder](json: InputStream): Resource[F, StreamingDecoderResult[T]] =
        readerResource(json).map { reader =>
          if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            Decoder[T].decode(reader)
          } else Left(StreamingDecodingFailure("Not an object!"))
        }

      def decodeKeyValues[T](
        json: InputStream
      )(implicit d: Decoder[T]): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
        readerResource(json).map { reader =>
          Stream
            .eval(Sync[F].delay { reader.beginObject(); reader })
            .flatMap(
              Stream.unfold(_) { reader =>
                if (reader.peek() == JsonToken.END_OBJECT) None
                else {
                  val key = reader.nextName()
                  Option(d.decode(reader).map(key -> _) -> reader)
                }
              }
            )
        }

      private def readerResource(json: InputStream): Resource[F, JsonReader] =
        Resource.fromAutoCloseable(Sync[F].delay(new JsonReader(new InputStreamReader(json))))

    }

  trait Decoder[A] {
    def decode(reader: JsonReader): StreamingDecoderResult[A]

    def map[B](f: A => B): Decoder[B] = reader => decode(reader).map(f)
  }

  object Decoder {
    def apply[A](implicit ev: Decoder[A]) = ev

    implicit val stringDecoder: Decoder[String] = createDecoder[String](JsonToken.STRING)(_.nextString())

    implicit val intDecoder: Decoder[Int] = createDecoder[Int](JsonToken.NUMBER)(_.nextInt())

    implicit val booleanDecoder: Decoder[Boolean] = createDecoder[Boolean](JsonToken.BOOLEAN)(_.nextBoolean())

    implicit val bigDecimalDecoder: Decoder[BigDecimal] =
      createDecoder[BigDecimal](JsonToken.NUMBER)(r => BigDecimal(r.nextString()))

    implicit val localDateDecoder: Decoder[LocalDate] =
      createDecoder[LocalDate](JsonToken.STRING)(r => LocalDate.parse(r.nextString()))

    implicit def optionDecoder[A](implicit aDecoder: Decoder[A]): Decoder[Option[A]] =
      reader => aDecoder.decode(reader).map(Option(_))

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
  }

  def createDecoder[A](token: JsonToken)(decode: JsonReader => A): Decoder[A] =
    reader =>
      if (reader.peek() == token)
        Either.catchNonFatal(decode(reader)).left.map(t => StreamingDecodingFailure(t.getMessage()))
      else Left(StreamingDecodingFailure(s"Expected ${token}, but got ${reader.peek()}"))

  trait Field {
    type Out
    def name: String
    def decoder: Decoder[Out]
  }

  // object Field {
  //   type Aux[O] = Field { type Out = O}
  // }

  // fieldnames, idea from: https://stackoverflow.com/a/53438635
  sealed trait FieldDecoder[T] {
    def fields: List[Field]
  }

  implicit val hnilFieldDecoder: FieldDecoder[HNil] =
    new FieldDecoder[HNil] {
      val fields: List[Field] = List.empty
    }

  implicit def hconsFieldDecoder[K <: Symbol, V, T <: HList](implicit
    witness: Witness.Aux[K],
    d: Decoder[V],
    rest: FieldDecoder[T]
  ): FieldDecoder[FieldType[K, V] :: T] =
    new FieldDecoder[FieldType[K, V] :: T] {
      def fields: List[Field] =
        new Field {
          type Out = V
          val name = witness.value.name
          val decoder = d
        } :: rest.fields
    }

  implicit def genericFieldDecoder[T, G](implicit
    lg: LabelledGeneric.Aux[T, G],
    rest: FieldDecoder[G]
  ): FieldDecoder[T] = {
    val (_) = (lg) // To avoid unused variable warning / error
    new FieldDecoder[T] {
      val fields: List[Field] = rest.fields
    }
  }

  trait DecodeFromMap[A] {
    def fromMap(map: Map[String, Any]): Either[StreamingDecoderError, A]
  }

  implicit def hnilDFM: DecodeFromMap[HNil] =
    new DecodeFromMap[HNil] {
      def fromMap(map: Map[String, Any]): Either[StreamingDecoderError, HNil] =
        Right(HNil)
    }

  implicit def hconsDecodeFromMap[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    ev: LowPriority,
    t: Lazy[DecodeFromMap[T]]
  ): DecodeFromMap[FieldType[K, H] :: T] = {
    val (_) = (ev)
    new DecodeFromMap[FieldType[K, H] :: T] {
      def fromMap(map: Map[String, Any]): StreamingDecoderResult[FieldType[K, H] :: T] =
        map
          .get(witness.value.name)
          .toRight(StreamingDecodingFailure(s"Couldn't find decoded value of ${witness.value.name}"))
          .flatMap { decoded =>
            decodeTail(decoded, t.value, map)
          }
    }
  }

  implicit def hconsOptionDecodeFromMap[K <: Symbol, H, T <: HList](implicit
    witness: Witness.Aux[K],
    t: Lazy[DecodeFromMap[T]]
  ): DecodeFromMap[FieldType[K, Option[H]] :: T] =
    new DecodeFromMap[FieldType[K, Option[H]] :: T] {
      def fromMap(map: Map[String, Any]): StreamingDecoderResult[FieldType[K, Option[H]] :: T] =
        map
          .get(witness.value.name)
          .fold(decodeTail(None, t.value, map)) { decoded =>
            decodeTail(decoded, t.value, map)
          }
    }

  @inline private def decodeTail[K <: Symbol, H, T <: HList](
    decoded: Any,
    t: DecodeFromMap[T],
    map: Map[String, Any]
  ): StreamingDecoderResult[FieldType[K, H] :: T] =
    t
      .fromMap(map)
      .map(tail =>
        shapeless.labelled.field[K](decoded.asInstanceOf[H]) :: tail
      ) // TODO: Find a way to get rid of `asInstanceOf`

  def deriveDecoder[A, H <: HList](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[A],
    mapper: DecodeFromMap[H]
  ): Decoder[A] =
    reader => {

      @tailrec
      def decodeKeys(
        reader: JsonReader,
        decodedFields: Either[StreamingDecoderError, Map[String, Any]]
      ): Either[StreamingDecoderError, Map[String, Any]] =
        if (reader.peek() == JsonToken.END_OBJECT) decodedFields
        else {
          val name = reader.nextName()
          fieldDecoders.fields
            .find(_.name == name) match {
            case None        =>
              reader.skipValue()
              decodeKeys(reader, decodedFields)
            case Some(field) =>
              decodeKeys(
                reader,
                for {
                  m       <- decodedFields
                  decoded <- field.decoder.decode(reader)
                } yield m + (name -> decoded)
              )
          }
        }

      reader.beginObject()
      val hlist = decodeKeys(reader, Right(Map.empty))
      while (reader.peek() == JsonToken.NAME) {
        reader.nextName()
        reader.skipValue()
      }
      reader.endObject()

      hlist.flatMap(mapper.fromMap(_)).map(lg.from(_))
    }
}
