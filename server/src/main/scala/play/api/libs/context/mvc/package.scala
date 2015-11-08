package play.api.libs.context

import play.api.mvc.RequestHeader

package object mvc {

  /**
    * A context extractor that operates on a Play Server [[play.api.mvc.Request]] or [[RequestHeader]].
    *
    * @tparam R the type of request to read the context from
    * @tparam Ctx the type of context to extract from the request
    * @tparam Errors the type of errors returned if the context cannot be found or parsed.
    */
  type RequestContextExtractor[-R <: RequestHeader, +Ctx, Errors] = ReadsRequestContext[R, Ctx, Errors]
}
