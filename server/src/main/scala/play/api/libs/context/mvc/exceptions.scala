package play.api.libs.context.mvc

import play.api.libs.context.ContextExtractException
import play.api.libs.json.{Json, Writes}

/**
  * An exception that can be thrown when failing to extract a context from a [[play.api.mvc.Request]].
  */
case class RequestContextParseException[Errors](
  message: String,
  errors: Errors,
  contextSource: RequestContextSource,
  cause: Option[Throwable] = None)
  (implicit writesErrors: Writes[Errors])
  extends ContextExtractException({
    val jsonErrors = writesErrors.writes(errors)
    s"$message. Encountered errors: ${Json.prettyPrint(jsonErrors)} [Context from ${contextSource.name}]"
  }, cause.orNull)
