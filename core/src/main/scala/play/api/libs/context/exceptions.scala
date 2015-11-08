package play.api.libs.context

import play.api.libs.json.{Json, Writes}

class ContextExtractException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)

/**
  * An exception that can be thrown when failing to extract a context from a [[play.api.mvc.Request]].
  */
case class DefaultContextExtractException[Errors](message: String, errors: Errors, cause: Throwable = null)
  (implicit writesErrors: Writes[Errors])
  extends ContextExtractException({
    val jsonErrors = writesErrors.writes(errors)
    s"$message. Encountered errors: ${Json.prettyPrint(jsonErrors)}"
  }, cause)
