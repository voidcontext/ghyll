package ghyll

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.unsafe.implicits.global
import fs2.Stream
import ghyll.json.JsonToken

trait TestTokenStream {
  def withTokens[A](tokens: List[JsonToken])(test: Stream[IO, JsonToken] => IO[A]): A =
    Ref
      .of[IO, List[JsonToken]](tokens)
      .flatMap { ref =>
        val stream =
          Stream.unfoldEval(ref) { ref =>
            ref.get.flatMap {
              case Nil       => IO.pure(None)
              case h :: tail => ref.set(tail).as(Some(h -> ref))
            }
          }

        test(stream)
      }
      .unsafeRunSync()

  def withTokenStream[A](tokens: List[JsonToken])(test: TokenStream[IO] => IO[A]): A =
    withTokens(tokens)(s => test(TokenStream.withPos(s)))

}
