package play.api.libs.context.ws

import play.api.http.Writeable
import play.api.libs.ws._

case class WrappedWSRequestWithContext[Ctx](withoutContext: WSRequest, context: Ctx)
  extends ProxyWSRequestWithContext[Ctx] {

  /**
    * Swaps context with the provided context
    */
  override def usingContext[N <: Ctx](newContext: N): WSRequestWithContext[N] =
    new WrappedWSRequestWithContext(withoutContext, newContext)

  /**
    * Wrap the resulting request in another proxy for receiving the Context variable.
    *
    * @param underlying the request to apply this type of context to.
    */
  override protected def proxy(underlying: WSRequest): WSRequestWithContext[Ctx] =
    new WrappedWSRequestWithContext(underlying, context)
}

trait ProxyWSRequestWithContext[Ctx] extends WSRequestWithContext[Ctx] {

  /**
    * Wrap the resulting request in another proxy for receiving the Context variable.
    *
    * @param underlying the request to apply this type of context to.
    */
  protected def proxy(underlying: WSRequest): WSRequestWithContext[Ctx]

  override def withContext(implicit infuser: WSRequestInfuser[Ctx]): WSRequest =
    infuser.infuse(withoutContext)(context)

  override def withMethod(method: String): WSRequestWithContext[Ctx] = proxy(withoutContext.withMethod(method))

  override def withQueryString(parameters: (String, String)*): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withQueryString(parameters: _*))

  override def withHeaders(hdrs: (String, String)*): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withHeaders(hdrs: _*))

  override def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withAuth(username, password, scheme))

  override def withRequestTimeout(timeout: Long): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withRequestTimeout(timeout))

  override def withBody(body: WSBody): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withBody(body))

  override def withBody[T](body: T)(implicit wrt: Writeable[T]): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withBody(body))

  override def withProxyServer(proxyServer: WSProxyServer): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withProxyServer(proxyServer))

  override def withFollowRedirects(follow: Boolean): WSRequestWithContext[Ctx] =
    proxy(withoutContext.withFollowRedirects(follow))

  override def withVirtualHost(vh: String): WSRequestWithContext[Ctx] = proxy(withoutContext.withVirtualHost(vh))

  override def sign(calc: WSSignatureCalculator): WSRequestWithContext[Ctx] = proxy(withoutContext.sign(calc))
}
