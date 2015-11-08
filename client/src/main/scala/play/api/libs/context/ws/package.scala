package play.api.libs.context

import play.api.libs.ws.WSRequest

/**
  * The context-passing client library supports Play WS by default.
  *
  * This library augments the WSClient with methods that require an implicit context value to make certain
  * requests. This is useful for when you need to always send some headers in requests, but don't want
  * to explicitly send these headers each time.
  */
package object ws extends WSContextSyntax {

  /**
    * A context infuser for adding the given type of context to a [[WSRequest]] to produce a [[WSRequest]].
    */
  type WSRequestInfuser[-Ctx] = ContextInfuser[Ctx, WSRequest, WSRequest]
}

