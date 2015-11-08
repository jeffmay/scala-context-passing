package play.api.libs.context.ws

import play.api.http.Writeable
import play.api.libs.context.ContextInfuser
import play.api.libs.ws._

object WSRequestWithContext {

  /**
    * Builds a [[WSRequestWithContext]] from the given request and implicit context.
    */
  def apply[Ctx](request: WSRequest)(implicit context: Ctx): WSRequestWithContext[Ctx] =
    new WrappedWSRequestWithContext(request, context)

  /**
    * Infuses the context with the request.
    *
    * @note Be sure not to override the context you just set up. It doesn't stay infused forever.
    *       On the flip side, you can use this to override values that would otherwise be applied last.
    *
    * @return a raw WSRequest with the context infused.
    */
  def infuse[Ctx](request: WSRequest)(implicit context: Ctx, infuser: WSRequestInfuser[Ctx]): WSRequest = {
    infuser.infuse(request)(context)
  }
}

trait WSRequestWithContext[Ctx] {

  /**
    * The current context
    */
  def context: Ctx

  /**
    * Swaps context with the provided context
    */
  def usingContext[N <: Ctx](newContext: N): WSRequestWithContext[N]

  /**
    * This request after the context has been applied.
    *
    * This is the magic sauce that infuses the request with the context. This and [[withoutContext]] are
    * the only way to access the underlying get, post, put, delete methods. If you choose to add the
    * context, then you need a way to infuse this context with the request.
    *
    * @note there is an implicit conversion to do just this, however, it is useful to have an explicit
    *       method for seeing the error message of not having the context infuser made explicit.
    */
  def withContext(implicit infuser: ContextInfuser[Ctx, WSRequest, WSRequest]): WSRequest

  /**
    * This request without any context applied
    *
    * @note this is how you can get around requiring a context when needing to make a request
    *       using a [[WSRequestWithContext]]
    */
  def withoutContext: WSRequest

  /**
    * sets the signature calculator for the request
    */
  def sign(calc: WSSignatureCalculator): WSRequestWithContext[Ctx]

  /**
    * sets the authentication realm
    */
  def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequestWithContext[Ctx]

  /**
    * adds any number of HTTP headers
    */
  def withHeaders(hdrs: (String, String)*): WSRequestWithContext[Ctx]

  /**
    * adds any number of query string parameters to the
    */
  def withQueryString(parameters: (String, String)*): WSRequestWithContext[Ctx]

  /**
    * Sets whether redirects (301, 302) should be followed automatically
    */
  def withFollowRedirects(follow: Boolean): WSRequestWithContext[Ctx]

  /**
    * Sets the maximum time in milliseconds you expect the request to take.
    * Warning: a stream consumption will be interrupted when this time is reached.
    */
  def withRequestTimeout(timeout: Long): WSRequestWithContext[Ctx]

  /**
    * Sets the virtual host to use in this request
    */
  def withVirtualHost(vh: String): WSRequestWithContext[Ctx]

  /**
    * Sets the proxy server to use in this request
    */
  def withProxyServer(proxyServer: WSProxyServer): WSRequestWithContext[Ctx]

  /**
    * Sets the body for this request
    */
  def withBody(body: WSBody): WSRequestWithContext[Ctx]

  /**
    * Sets the body for this request
    */
  def withBody[T](body: T)(implicit wrt: Writeable[T]): WSRequestWithContext[Ctx]

  /**
    * Sets the method for this request
    */
  def withMethod(method: String): WSRequestWithContext[Ctx]
}
