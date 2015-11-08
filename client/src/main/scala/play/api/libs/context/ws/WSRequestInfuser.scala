package play.api.libs.context.ws

import play.api.libs.ws.WSRequest

object WSRequestInfuser {

  /**
    * Builds a [[WSRequest]] infuser from a function that takes the context and produces
    * an update function.
    */
  def apply[Ctx](applyContext: Ctx => WSRequest => WSRequest): WSRequestInfuser[Ctx] = {
    new WSRequestInfuser[Ctx] {
      override def infuse(request: WSRequest)(implicit context: Ctx): WSRequest = {
        applyContext(context)(request)
      }
    }
  }

  /**
    * Builds a [[WSRequest]] infuser from a function that takes the context and produces
    * a sequence of headers to add.
    */
  def addToHeaders[Ctx](infuse: Ctx => Seq[(String, String)]): WSRequestInfuser[Ctx] = {
    WSRequestInfuser { context =>
      val headersFromContext = infuse(context)
      _.withHeaders(headersFromContext: _*)
    }
  }
}
