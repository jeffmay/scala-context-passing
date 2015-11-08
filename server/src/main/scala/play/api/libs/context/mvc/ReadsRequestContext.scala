package play.api.libs.context.mvc

import org.scalactic.{Bad, Every, Good, Or}
import play.api.libs.context.ContextExtractor
import play.api.libs.context.functional.Show
import play.api.libs.context.json._
import play.api.libs.context.show._
import play.api.libs.json.Writes
import play.api.mvc._

import scala.reflect.ClassTag

/**
  * A context extractor that reads Play Server [[Request]]s.
  *
  * @tparam R the type of request required to use this extractor
  * @tparam Ctx the type of context to extract
  * @tparam Errors the type of error(s) that can be returned instead. Note, this is plural
  *                because it is rare to go from unstructured data to structured data without
  *                needing the ability to accumulate all the errors.
  *                I strongly encourage the [[org.scalactic.Every]] type here to wrap your errors.
  */
trait ReadsRequestContext[-R <: RequestHeader, +Ctx, Errors]
  extends ContextExtractor[R, Ctx, Errors] {
  outer =>

  /**
    * Creates a reader that maps the context of the underlying reader using the same source and show for errors.
    */
  def map[NextCtx](f: Ctx => NextCtx): ReadsRequestContext[R, NextCtx, Errors] = {
    new ReadsRequestContext[R, NextCtx, Errors] {
      override def contextSource: RequestContextSource = outer.contextSource
      override def extractOrResponse(request: R): NextCtx Or Result = outer.extractOrResponse(request).map(f)
      override def extractOrErrors(source: R): NextCtx Or Errors = outer.extractOrErrors(source).map(f)
      override def extractOrThrow(source: R): NextCtx = f(outer.extractOrThrow(source))
      override implicit def showErrors: Show[Errors] = outer.showErrors
    }
  }

  /**
    * Creates a reader that reads the request in order to pick the next request reader.
    */
  def flatMap[NextCtx, NewR <: R](f: Ctx => ReadsRequestContext[NewR, NextCtx, Errors]): ReadsRequestContext[NewR, NextCtx, Errors] = {
    new ReadsRequestContext[NewR, NextCtx, Errors] {
      override def contextSource: RequestContextSource = outer.contextSource
      override def extractOrResponse(request: NewR): NextCtx Or Result = {
        outer.extractOrResponse(request) match {
          case Good(ctx) => f(ctx).extractOrResponse(request)
          case Bad(response) => Bad(response)
        }
      }
      override def extractOrErrors(source: NewR): NextCtx Or Errors = {
        outer.extractOrErrors(source) match {
          case Good(ctx) => f(ctx).extractOrErrors(source)
          case Bad(errors) => Bad(errors)
        }
      }
      override def extractOrThrow(source: NewR): NextCtx = {
        f(outer.extractOrThrow(source)).extractOrThrow(source)
      }
      override implicit def showErrors: Show[Errors] = outer.showErrors
    }
  }

  /**
    * The part or parts of the request from which the context is being extracted.
    */
  def contextSource: RequestContextSource

  /**
    * Reads the context from the request or produces an immediate response.
    *
    * Since context should be extracted from the request immediately, we can safely
    * assume that there is always a way to generate a response for a bad request or
    * at least provide some dummy value that satisfies the required context.
    *
    * @note the behavior of this method is not required to be the same as [[extractOrErrors]].
    *       It is conceivable that one would want to recover from the errors in a different manner
    *       than just producing a Result or generating a dummy context.
    */
  def extractOrResponse(request: R): Ctx Or Result
}

object ReadsRequestContext {

  /**
    * A [[ReadsRequestContext]] that extracts the context from the headers or produces a collection of [[HeaderError]].
    */
  type FromHeaders[+Ctx] = ReadsRequestContext[RequestHeader, Ctx, Every[HeaderError]]

  /**
    * Create a common exception for when a [[Request]] extractor fails.
    *
    * @param reader the context reader (used for type information and the context source)
    * @param request the request from which the context could not be extracted
    * @param errors the errors that were encountered when extracting the context
    * @param tag the class tag of the context for exception information
    * @return a [[RequestContextParseException]] carrying a well documented exception message
    */
  def badContextException[Ctx, Errors: Writes](
    reader: ReadsRequestContext[_, Ctx, Errors],
    request: RequestHeader,
    errors: Errors)
    (implicit tag: ClassTag[Ctx]): RequestContextParseException[Errors] = {
    val className = tag.runtimeClass.getName
    val exceptionMessage = s"Could not read an instance of $className from $request"
    RequestContextParseException[Errors](exceptionMessage, errors, reader.contextSource)
  }

  /**
    * Summon an implicit [[FromHeaders]] reader.
    */
  def fromHeaders[Ctx](implicit reader: FromHeaders[Ctx]): FromHeaders[Ctx] = reader

  /**
    * Creates an intermediary [[FromHeadersBuilder]] for building the final [[ReadsRequestContext.FromHeaders]].
    */
  def usingHeaders[Ctx: ClassTag](fromHeaders: HeaderReader => Ctx Or Every[HeaderError]): FromHeadersBuilder[Ctx] = {
    new FromHeadersBuilder[Ctx](req => fromHeaders(new HeaderReader(req)))
  }

  /**
    * Creates an intermediary [[FromHeadersBuilder]] for building the final [[ReadsRequestContext.FromHeaders]].
    *
    * @note this is used to pick the right [[FromHeaders]] reader based on the value of a determiner header.
    *       It is really just a convenience method for building an abstract reader based on some key header.
    */
  def chooseFromHeader[Ctx: ClassTag](headerName: String)(fromHeaders: Option[String] => FromHeaders[Ctx]): FromHeadersBuilder[Ctx] = {
    new FromHeadersBuilder[Ctx]({ req =>
      fromHeaders(new HeaderReader(req).get(headerName).toOption).extractOrErrors(req)
    })
  }

  /**
    * An immutable builder for better syntax without encountering type erasure issues with overloaded methods.
    */
  class FromHeadersBuilder[Ctx: ClassTag] private[ReadsRequestContext] (
    reads: RequestHeader => Ctx Or Every[HeaderError]
  ) {
    outer =>

    /**
      * Maps over the resulting [[FromHeadersBuilder]] and combines the recover block.
      */
    def map[NewCtx: ClassTag](f: Ctx => NewCtx): FromHeadersBuilder[NewCtx] = {
      new FromHeadersBuilder[NewCtx]({ headers =>
        outer.reads(headers) match {
          case Good(ctx) => Good(f(ctx))
          case Bad(errors) => Bad(errors)
        }
      })
    }

    /**
      * Maps a function that determines the final [[FromHeadersBuilder]] and combines the errors with the recover block.
      */
    def flatMap[NewCtx: ClassTag](f: Ctx => FromHeaders[NewCtx]): FromHeadersBuilder[NewCtx] = {
      new FromHeadersBuilder[NewCtx]({ request =>
        outer.reads(request) match {
          case Good(ctx) => f(ctx).extractOrErrors(request)
          case Bad(errors) => Bad(errors)
        }
      })
    }

    /**
      * Builds a [[ReadsRequestContext.FromHeaders]] provided a function to recover either a context
      * or a [[Result]] (if no context can be recovered).
      *
      * @note this can be used recover from any errors with a dummy value for the context.
      */
    def recoverWith(recoverWith: Every[HeaderError] => Ctx Or Result): ReadsRequestContext.FromHeaders[Ctx] = {
      new ReadsRequestContextFromHeaders[Ctx](reads, recoverWith)
    }

    /**
      * Builds a [[ReadsRequestContext.FromHeaders]] provided a function to recover a [[Result]]
      * from any group of header errors.
      *
      * @note this is similar to [[recoverWith]], except that there is no way to recover a context
      *       from the errors (or a dummy context).
      */
    def recoverResult(recover: Every[HeaderError] => Result): ReadsRequestContext.FromHeaders[Ctx] = {
      new ReadsRequestContextFromHeaders[Ctx](reads, { errors: Every[HeaderError] =>
        Bad(recover(errors))
      })
    }

    /**
      * Builds a [[ReadsRequestContext.FromHeaders]] provided a backup [[Result]] value for
      * if the extraction fails.
      */
    def orResult(result: => Result): ReadsRequestContext.FromHeaders[Ctx] = {
      new ReadsRequestContextFromHeaders[Ctx](reads, _ => Bad(result))
    }
  }
}

/**
  * An implementation that provides the default implementations for methods using constructor arguments.
  */
abstract class DefaultWSRequestContextReads[-R <: RequestHeader, +Ctx: ClassTag, Errors: Writes](
  override val contextSource: RequestContextSource,
  recoverWith: Errors => Ctx Or Result
)(
  implicit override val showErrors: Show[Errors]
) extends ReadsRequestContext[R, Ctx, Errors] {

  override def extractOrThrow(request: R): Ctx = {
    extractOrErrors(request) match {
      case Good(ctx) => ctx
      case Bad(errors) => throw ReadsRequestContext.badContextException(this, request, errors)
    }
  }

  override def extractOrResponse(request: R): Or[Ctx, Result] = {
    extractOrErrors(request).recoverWith(recoverWith)
  }
}

/**
  * A [[ContextExtractor]] that extracts the context from the [[RequestHeader]]'s [[Headers]].
  */
class ReadsRequestContextFromHeaders[Ctx: ClassTag](
  requestReader: RequestHeader => Ctx Or Every[HeaderError],
  recoverWith: Every[HeaderError] => Ctx Or Result
) extends DefaultWSRequestContextReads[RequestHeader, Ctx, Every[HeaderError]](
  ContextFromHeaders,
  recoverWith
) {

  override def extractOrErrors(request: RequestHeader): Ctx Or Every[HeaderError] = requestReader(request)
}
