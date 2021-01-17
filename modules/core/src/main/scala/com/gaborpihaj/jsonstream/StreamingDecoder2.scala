package com.gaborpihaj.jsonstream

import java.io.{InputStream, InputStreamReader}

import cats.effect.{Resource, Sync}
import cats.syntax.either._
import com.google.gson.stream.{JsonReader, JsonToken}
import fs2.Stream
import shapeless._
import shapeless.labelled.FieldType

@SuppressWarnings(Array("scalafix:DisableSyntax.==", "scalafix:DisableSyntax.asInstanceOf"))
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

      def decodeKeyValues[T: Decoder](json: InputStream): Resource[F, Stream[F, StreamingDecoderResult[(String, T)]]] =
        ???

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
      def fields: List[Field] = List.empty
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
  ): FieldDecoder[T] =
    new FieldDecoder[T] {
      def fields: List[Field] = {
        val (_) = (lg) // To avoid unused variable warning / error
        rest.fields
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
    t: Lazy[DecodeFromMap[T]]
  ): DecodeFromMap[FieldType[K, H] :: T] =
    new DecodeFromMap[FieldType[K, H] :: T] {
      def fromMap(map: Map[String, Any]): Either[StreamingDecoderError, FieldType[K, H] :: T] =
        map
          .get(witness.value.name)
          .toRight(StreamingDecodingFailure(s"Couldn't find decoded value of ${witness.value.name}"))
          .flatMap { decoded =>
            t.value
              .fromMap(map)
              .map(tail =>
                shapeless.labelled.field[K](decoded.asInstanceOf[H]) :: tail
              ) // TODO: Find a way to get rid of `asInstanceOf`
          }
    }

  def deriveDecoder[A, H <: HList](implicit
    lg: LabelledGeneric.Aux[A, H],
    fieldDecoders: FieldDecoder[A],
    mapper: DecodeFromMap[H]
  ): Decoder[A] =
    reader => {

      def decodeKeys(
        reader: JsonReader,
        decodedFields: Either[StreamingDecoderError, Map[String, Any]]
      ): Either[StreamingDecoderError, Map[String, Any]] =
        if (reader.peek() == JsonToken.END_OBJECT) decodedFields
        else
          decodedFields.flatMap { m =>
            val name = reader.nextName()
            val field = fieldDecoders.fields.find(_.name == name)

            decodeKeys(
              reader,
              for {
                f <- field.toRight(StreamingDecodingFailure(s"Couldn't find decoder for field $name"))
                d <- f.decoder.decode(reader)
              } yield m + (name -> d)
            )

          }

      reader.beginObject()
      val hlist = decodeKeys(reader, Right(Map.empty))
      reader.endObject()

      hlist.flatMap(mapper.fromMap(_)).map(lg.from(_))
    }
}
