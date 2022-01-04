package ghyll.json

import cats.effect.{IO, Ref}
import ghyll.{Pos, TokeniserError}

trait TestJsonTokenReader[F[_]] extends JsonTokenReader[F]

object TestJsonTokenReader {
  def withTokens(tokens: List[JsonToken]): IO[TestJsonTokenReader[IO]] =
    Ref.of[IO, (List[Pos], List[JsonToken])](Nil -> tokens).map { tokensRef =>
      new TestJsonTokenReader[IO] {

        def next: IO[Either[TokeniserError, (List[Pos], JsonToken)]] =
          tokensRef.getAndUpdate {
            case (stack, head :: tail) =>
              JsonToken.pos(head, stack) -> tail
            case _                     => ???
          }.map {
            case (stack, head :: _) => Right(stack -> head)
            case _                  => ???
          }
      }
    }
}
