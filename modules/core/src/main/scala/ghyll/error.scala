package ghyll

trait TokeniserError
case class NestingError(message: String) extends TokeniserError

