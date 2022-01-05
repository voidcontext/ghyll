package ghyll.json

import cats.effect.{IO, Ref}

trait TestJsonTokenWriter[F[_]] extends JsonTokenWriter[F] {
  def written: F[List[JsonToken]]
}

object TestJsonTokenWriter {
  def apply: IO[TestJsonTokenWriter[IO]] =
    Ref.of[IO, List[JsonToken]](Nil).map { writtenRef =>
      new TestJsonTokenWriter[IO] {

        def write(token: JsonToken): IO[Unit] =
          writtenRef.update(token :: _)

        def written: IO[List[JsonToken]] =
          writtenRef.get.map(_.reverse)

      }
    }
}
