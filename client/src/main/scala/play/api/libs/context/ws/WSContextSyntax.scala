package play.api.libs.context.ws

import play.api.libs.ws.WSRequest

import scala.language.implicitConversions

/**
  * Provides the syntax to add implicit context types to requests.
  */
trait WSContextSyntax {

  /**
    * Adds with .withContext method to requests without any context applied.
    */
  implicit def reqWithContext(req: WSRequest): WSRequestContextBuilder = new WSRequestContextBuilder(req)
}

class WSRequestContextBuilder(val req: WSRequest) extends AnyVal {

  /**
    * Adds the implicit context type to the request to add type-safe context passing with [[WSRequestWithContext]].
    */
  def withContext[Ctx](implicit context: Ctx): WSRequestWithContext[Ctx] = WSRequestWithContext(req)
}