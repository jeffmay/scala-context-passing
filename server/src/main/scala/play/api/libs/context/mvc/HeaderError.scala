package play.api.libs.context.mvc

import org.scalactic.{Every, Many, One}
import play.api.libs.context.functional.Show
import play.api.libs.context.json.ShowJson
import play.api.libs.context.show._
import play.api.libs.json.{Json, Writes}

import scala.reflect.ClassTag

/**
  * An error when attempting to parse a header from a request.
  */
sealed trait HeaderError {
  def headerName: String
  def message: String
}
object HeaderError {
  /**
    * The expected header name is missing.
    */
  def missing(headerName: String): MissingHeaderError = new MissingHeaderError(headerName)

  /**
    * There was an error parsing the header that was found with the given header name, but it couldn't
    * be parsed as an instance of the expected class, with the message and optional cause provided.
    *
    * The error message will include both the header name, value, error message, and cause.
    */
  def error[T](headerName: String, headerValue: String, message: String, cause: Throwable = null)
    (implicit tag: ClassTag[T]): HeaderParseError = {
    new HeaderParseError(headerName, headerValue, tag.getClass.getName, message, Option(cause))
  }

  implicit val writesHeaderError: Writes[HeaderError] = Writes(error => Json.obj(error.headerName -> error.message))

  implicit val showHeaderError: Show[HeaderError] = ShowJson.asPrettyString[HeaderError]

  // These would be provided by importing the default show implicits,
  // but I put them here since it will be common to need a show of these types.
  implicit val showOneHeaderError: Show[One[HeaderError]] = showOne[HeaderError]
  implicit val showManyHeaderErrors: Show[Many[HeaderError]] = showMany[HeaderError]
  implicit val showEveryHeaderError: Show[Every[HeaderError]] = showEvery[HeaderError]

  implicit def showMultipleErrors[C <: Traversable[HeaderError]]: Show[C] = Show.show { errors =>
    val errorMap: Map[String, Seq[String]] = errors.groupBy(_.headerName).mapValues(keyErrors => keyErrors.map(_.message).toSeq)
    val jsonErrors = Json.toJson(errorMap)
    Json.prettyPrint(jsonErrors)
  }
}

/**
  * The expected header was missing from the request.
  */
case class MissingHeaderError(headerName: String) extends HeaderError {
  override def message: String = s"missing header '$headerName'"
}

/**
  * An error was encountered parsing the header value.
  */
case class HeaderParseError(
  headerName: String,
  headerValue: String,
  expectedTypeName: String,
  error: String,
  cause: Option[Throwable]) extends HeaderError {
  override def message: String =
    s"Expected header of type $expectedTypeName, found '$headerValue'. Error: $error" +
      cause.fold("")(" [" + _.getMessage + "]")
}
