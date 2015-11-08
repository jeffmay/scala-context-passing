package play.api.libs.context.ws

import play.api.libs.ws.{WSClient, WSRequest}

/**
  * Like [[ContextPassingWSClient]] except that it puts a lower bound on the types of request
  * contexts that can be used.
  *
  * This is useful when you want to leverage type inference and some type safety around the
  * type of context required to make HTTP calls.
  *
  * @tparam Min the lower boundary of the type required for the context
  */
trait MinimumContextWSClient[-Min] {

  /**
    * Returns this client without the minimum type restriction.
    *
    * @note this should be a rare case. It would be far more likely that you would start
    *       with a [[ContextPassingWSClient]] and then add a minimum type restriction,
    *       but I'm including this for debugging purposes.
    */
  def withoutContextType: ContextPassingWSClient

  /**
    * Generates a request which can be used to build requests.
    *
    * @param url The base URL to make HTTP requests to.
    * @return a WSRequestHolder
    */
  def url[Ctx <: Min](url: String)(implicit context: Ctx): WSRequestWithContext[Ctx]

  /**
    * Generates a request with the context already infused.
    *
    * @note the preferred method is to use the other url method and call the
    *       [[WSRequestWithContext.withContext]] method on the result as it prevents you from
    *       clobbering your own context by mistake. This is provided as a short-hand for when
    *       you just want to pass the context explicitly.
    */
  def urlWithContext[Ctx <: Min](url: String, context: Ctx)(implicit infuser: WSRequestInfuser[Ctx]): WSRequest

  /**
    * Closes this client, and releases underlying resources.
    */
  def close(): Unit
}

case class WrappedMinimumContextWSClient[Min](withoutContext: WSClient) extends MinimumContextWSClient[Min] {

  override def withoutContextType: ContextPassingWSClient = new WrappedContextPassingWSClient(withoutContext)

  override def url[Ctx <: Min](url: String)(implicit context: Ctx): WSRequestWithContext[Ctx] = {
    WSRequestWithContext[Ctx](withoutContext.url(url))
  }

  def urlWithContext[Ctx <: Min](url: String, context: Ctx)(implicit infuser: WSRequestInfuser[Ctx]): WSRequest = {
    infuser.infuse(withoutContext.url(url))(context)
  }

  override def close(): Unit = withoutContext.close()
}
