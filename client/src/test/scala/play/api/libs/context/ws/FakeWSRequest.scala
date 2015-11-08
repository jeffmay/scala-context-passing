package play.api.libs.context.ws

import java.net.URL

import play.api.libs.iteratee.Enumerator
import play.api.libs.ws._

import scala.concurrent.Future

case class FakeWSRequest(
  response: Future[WSResponse] = Future.failed(new NotImplementedError),
  method: String = "GET",
  validUrl: URL = new URL("http", "localhost", "/"),
  headers: Map[String, Seq[String]] = Map.empty,
  queryString: Map[String, Seq[String]] = Map.empty,
  body: WSBody = InMemoryBody(Array.empty),
  responseStream: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = Future.failed(new NotImplementedError),
  auth: Option[(String, String, WSAuthScheme)] = None,
  requestTimeout: Option[Int] = None,
  followRedirects: Option[Boolean] = None,
  calc: Option[WSSignatureCalculator] = None,
  proxyServer: Option[WSProxyServer] = None,
  virtualHost: Option[String] = None
) extends WSRequest {

  override val url: String = validUrl.toExternalForm

  def withResponse(response: Future[WSResponse]): FakeWSRequest = copy(response = response)

  override def withHeaders(hdrs: (String, String)*): WSRequest = {
    val headers = hdrs.foldLeft(this.headers)((m, hdr) =>
      if (m.contains(hdr._1)) m.updated(hdr._1, m(hdr._1) :+ hdr._2)
      else m + (hdr._1 -> Seq(hdr._2))
    )
    copy(headers = headers)
  }

  override def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequest = {
    copy(auth = Some(username, password, scheme))
  }

  override def withQueryString(parameters: (String, String)*): WSRequest = {
    copy(queryString = parameters.foldLeft(this.queryString) {
      case (m, (k, v)) => m + (k -> (v +: m.getOrElse(k, Nil)))
    })
  }

  override def execute(): Future[WSResponse] = response

  override def sign(calc: WSSignatureCalculator): WSRequest = copy(calc = Some(calc))

  override def stream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = responseStream

  override def withVirtualHost(vh: String): WSRequest = copy(virtualHost = Some(vh))

  override def withMethod(method: String): WSRequest = copy(method = method)

  override def withRequestTimeout(timeout: Long): WSRequest = copy(requestTimeout = Some(timeout.toInt))

  override def withProxyServer(proxyServer: WSProxyServer): WSRequest = copy(proxyServer = Some(proxyServer))

  override def withFollowRedirects(follow: Boolean): WSRequest = copy(followRedirects = Some(follow))

  override def withBody(body: WSBody): WSRequest = copy(body = body)
}
