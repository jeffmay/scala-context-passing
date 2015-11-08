package play.api.libs.context.ws

import play.api.libs.ws.{WSRequest, WSClient}

/**
  * A [[WSClient]]-like interface that requires you to specify the type of context you want
  * before each request.
  *
  * Unlike [[MinimumContextWSClient]], this has no restriction on type, but can't benefit from
  * type inference to provide the context.
  */
trait ContextPassingWSClient {

  /**
    * Access the underlying client without the context.
    */
  def withoutContext: WSClient

  /**
    * Switch over to a WS client that requires a minimum context type, but benefits from type inference.
    *
    * @tparam Ctx the lower bound of the type of context that can be provided.
    */
  def withContextType[Ctx]: MinimumContextWSClient[Ctx]

  /**
    * Generates a request which can be used to build requests, so long as the context
    * is provided.
    */
  def url[Ctx](url: String)(implicit context: Ctx): WSRequestWithContext[Ctx]

  /**
    * Generates a request with the context already infused.
    *
    * @note the preferred method is to use the other url method and call the
    *       [[WSRequestWithContext.withContext]] method on the result as it prevents you from
    *       clobbering your own context by mistake. This is provided as a short-hand for when
    *       you just want to pass the context explicitly.
    */
  def urlWithContext[Ctx](url: String, context: Ctx)(implicit infuser: WSRequestInfuser[Ctx]): WSRequest

  /**
    * Closes this client, and releases underlying resources.
    */
  def close(): Unit
}

case class WrappedContextPassingWSClient(withoutContext: WSClient) extends ContextPassingWSClient {

  override def withContextType[Ctx]: MinimumContextWSClient[Ctx] = {
    new WrappedMinimumContextWSClient[Ctx](withoutContext)
  }

  override def url[Ctx](url: String)(implicit context: Ctx): WSRequestWithContext[Ctx] = {
    WSRequestWithContext(withoutContext.url(url))
  }

  def urlWithContext[Ctx](url: String, context: Ctx)(implicit infuser: WSRequestInfuser[Ctx]): WSRequest = {
    infuser.infuse(withoutContext.url(url))(context)
  }

  override def close(): Unit = withoutContext.close()
}