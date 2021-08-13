package ghyll

// import cats.effect.kernel.Ref
// import cats.effect.unsafe.implicits.global
import ghyll.json.JsonToken

trait TestTokenStream {
  // def withTokens[A](tokens: List[JsonToken])(test: Stream[IO, JsonToken] => IO[A]): A = ???
  //   // Ref
  //   //   .of[IO, List[JsonToken]](tokens)
  //   //   .flatMap { ref =>
  //   //     val stream =
  //   //       Stream.unfoldEval(ref) { ref =>
  //   //         ref.get.flatMap {
  //   //           case Nil       => IO.pure(None)
  //   //           case h :: tail => ref.set(tail).as(Some(h -> ref))
  //   //         }
  //   //       }

  //   //     test(stream)
  //   //   }
  //   //   .unsafeRunSync()

  def wrapTokens(tokens: List[JsonToken]): LazyList[Either[TokeniserError, JsonToken]] =
    LazyList(tokens.map(Right(_)): _*)

  def tokenStream(tokens: List[JsonToken]): TokenStream =
    TokenStream.withPos(LazyList(tokens.map(Right(_)): _*))

  // def withTokenStream[A](tokens: List[JsonToken])(test: TokenStream => A): A =
  //   test(tokenStream(tokens))

}
