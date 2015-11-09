package example.clients

import example.models.{AnySession, AuthSession, User}
import example.util.JsonAsOrThrow
import play.api.libs.context.ws.MinimumContextWSClient

import scala.concurrent.{ExecutionContext, Future}

class ExampleClient(authWs: MinimumContextWSClient[AnySession])(implicit ec: ExecutionContext) extends JsonAsOrThrow {

  val ws = authWs

  def viewUserImplicit(otherUserId: String)(implicit auth: AuthSession): Future[User] = {
    authWs.url(s"/rest/profile/$otherUserId").withContext.get().map {
      resp => resp.json.asOrThrow[User]
    }
  }

  def viewUserExplicit(auth: AuthSession, otherUserId: String): Future[User] = {
    authWs.urlWithContext(s"/rest/profile/$otherUserId", auth).get().map {
      resp => resp.json.asOrThrow[User]
    }
  }

  def viewUserEvenMoreExplicit(auth: AuthSession, otherUserId: String): Future[User] = {
    authWs.urlWithContext(s"/rest/profile/$otherUserId", auth)(AnySession.addToHeaders).get().map {
      resp => resp.json.asOrThrow[User]
    }
  }
}
