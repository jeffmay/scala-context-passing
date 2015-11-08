package play.api.libs.context.util

import java.nio.charset.Charset
import java.util.UUID

import org.scalactic.Or
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.util.Try

/**
  * A typeclass for things that can be safely parsed from strings.
  *
  * @tparam T the type of model to parse from a string.
  */
trait StringParser[T] {

  /**
    * Parse the string if possible, otherwise provide a helpful error value.
    */
  def parse(str: String): T Or StringParserError
}

object StringParser {

  /**
    * Parse a string as the expected type or an error using the implicit parser.
    */
  def parse[T](str: String)(implicit parser: StringParser[T]): T Or StringParserError = parser.parse(str)

  /**
    * Parse a string as the expected type or None using the implicit parser.
    */
  def parseOpt[T](str: String)(implicit parser: StringParser[T]): Option[T] = parser.parse(str).toOption

  /**
    * Parse a string as the expected type or an exception using the implicit parser.
    */
  def parseTry[T](str: String)(implicit parser: StringParser[T]): Try[T] = {
    parser.parse(str).badMap(error => new StringParserException(error)).toTry
  }

  /**
    * Parse a string as the expected type or throw an exception using the implicit parser.
    */
  def parseOrThrow[T](str: String)(implicit parser: StringParser[T]): T = parseTry[T](str).get

  /**
    * The default charset for everything (usually).
    */
  val UTF8: Charset = Charset.forName("UTF-8")

  implicit def ParseBinary(implicit charset: Charset = UTF8) = new DefaultStringParser("Array[Byte]", _.getBytes(charset))
  implicit object ParseBoolean extends DefaultStringParser("Boolean", _.toBoolean)
  implicit object ParseShort extends DefaultStringParser("Short", _.toShort)
  implicit object ParseInt extends DefaultStringParser("Int", _.toInt)
  implicit object ParseLong extends DefaultStringParser("Long", _.toLong)
  implicit object ParseFloat extends DefaultStringParser("Float", _.toFloat)
  implicit object ParseDouble extends DefaultStringParser("Double", _.toDouble)
  implicit object ParseString extends DefaultStringParser[String]("String", identity)
  implicit object ParseUUID extends DefaultStringParser[UUID]("UUID", UUID.fromString)
  implicit object ParseJsValue extends DefaultStringParser[JsValue]("JsValue", Json.parse)
}

/**
  * A simple implementation of [[StringParser]] that handles exceptions.
  */
class DefaultStringParser[T](typeName: String, read: String => T) extends StringParser[T] {
  override def parse(str: String): T Or StringParserError = {
    Or.from(Try(read(str))).badMap(ex => new StringParserError(str, typeName, None, Some(ex)))
  }
}

/**
  * An error from attempting to parse a string with a nicely formatted error message.
  */
case class StringParserError(value: String, expectedType: String, details: Option[String] = None, cause: Option[Throwable] = None) {
  lazy val message: String = {
    val reason = details.fold("")(": " + _) + cause.fold("")(" (Encountered " + _.getMessage + ")")
    s"'$value' could not be parsed as $expectedType$reason"
  }
}

/**
  * An exception wrapper around [[StringParserError]], in case you want to throw it.
  */
case class StringParserException(error: StringParserError) extends RuntimeException(error.message, error.cause.orNull)
