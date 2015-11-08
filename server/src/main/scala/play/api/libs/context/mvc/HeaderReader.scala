package play.api.libs.context.mvc

import org.scalactic.{One, Or}
import play.api.libs.context.util.StringParser
import play.api.mvc.{RequestHeader, Headers}

import scala.reflect.ClassTag
import scala.util.Try

/**
  * A wrapper for extracting headers from a [[play.api.mvc.Request]]'s [[Headers]] with errors that
  * better match the expected type for [[ReadsRequestContext.FromHeaders]].
  *
  * Provides the following syntax:
  *
  * {{{
  *   ReadsRequestContext.fromHeaders { headers =>
  *     for {
  *       stringHeader <- headers.get("stringHeader")
  *       longHeader <- headers.getAs[Long]("longHeader")
  *       durationHeader <- headers.tryAs[Duration]("durationHeader")(Duration(_))
  *     } yield Ctx(stringHeader, longHeader, durationHeader)
  *   }
  * }}}
  *
  * @param headers the headers from which to get header values.
  */
class HeaderReader(headers: Headers) {

  def this(request: RequestHeader) = this(request.headers)

  /**
    * Extracts the header as a String or a single [[HeaderError]].
    */
  def get(headerName: String): String Or One[HeaderError] = {
    Or.from(headers.get(headerName), One(HeaderError.missing(headerName)))
  }

  /**
    * Extracts the header as the given type that has an implicit [[StringParser]].
    *
    * @note this is similar to [[tryAs]] except that the parser is grabbed implicitly, so it is likely
    *       to be shared with other parsers.
    */
  def getAs[T: ClassTag](headerName: String)(implicit parser: StringParser[T]): T Or One[HeaderError] = {
    get(headerName).flatMap { headerValue =>
      parser.parse(headerValue).badMap { error =>
        One(HeaderError.error[T](headerName, headerValue, error.message, error.cause.orNull))
      }
    }
  }

  /**
    * Extracts the header as the given type using the provided parsing function that should
    * either produce the correct type or throw an exception.
    *
    * @note if the parse function throws an exception, it will be wrapped un a header error
    *       using the [[HeaderError.error]] method.
    */
  def tryAs[T: ClassTag](headerName: String)(parse: String => T): T Or One[HeaderError] = {
    get(headerName).flatMap { headerValue =>
      Or.from(Try(parse(headerValue))).badMap { ex =>
        One(HeaderError.error[T](headerName, headerValue, "exception thrown in provided HeaderReader.tryAs parse function", ex))
      }
    }
  }
}
