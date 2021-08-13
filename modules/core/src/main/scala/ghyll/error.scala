package ghyll

trait TokeniserError
case class NestingError(message: String) extends TokeniserError
case object LazyHead extends TokeniserError
case object UnimplementedToken extends TokeniserError
case object JsonPathError extends TokeniserError

