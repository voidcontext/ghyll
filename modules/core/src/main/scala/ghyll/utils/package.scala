package ghyll

package object utils {
  implicit class EitherOps[A](value: A) {
    def left[R]: Either[A, R] = Left(value)

    def right[L]: Either[L, A] = Right(value)
  }
}
