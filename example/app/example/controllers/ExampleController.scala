package example.controllers

import example.cache.UserClient
import example.models.{AuthSession, AnySession}
import play.api.libs.context.Context
import play.api.libs.context.mvc.ActionWithContext
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}

class ExampleController(client: UserClient)(implicit ec: ExecutionContext) extends Controller {

  def raw = ActionWithContext.syncRaw {
    Ok("totally raw")
  }

  def rawSync = ActionWithContext.syncRaw {
    request =>
      Ok(s"raw with request $request")
  }

  def rawAsync = ActionWithContext.asyncRaw {
    request =>
      Future.successful(Ok(s"async raw with request $request"))
  }

  def unauthenticatedSync = ActionWithContext.fromHeaders[AnySession].sync { implicit ctx =>
    request =>
      Ok(s"unauthenticated (SessionId = ${ctx.sessionId}) sync request $request")
  }

  def unauthenticatedAsync = ActionWithContext.fromHeaders[AuthSession].async { implicit ctx =>
    request =>
      for {
        user <- client.fetchUser
      } yield {
        Ok(s"unauthenticated (SessionId = ${ctx.sessionId}) sync request $request")
      }
  }

  def authenticatedAsync = ActionWithContext.fromHeaders[AuthSession].async { implicit ctx =>
    request =>
      for {
        user <- client.fetchUser.recoverWith {
          case ex => Future.failed(new IllegalArgumentException("whatever"))
        }
      } yield {
        Ok(s"unauthenticated (SessionId = ${ctx.sessionId}) sync request $request")
      }
  }

  def explicitlyExtract = ActionWithContext.asyncRaw {
    request =>
      val session = Context.from(request).extractOrThrow[AuthSession]
      Future.successful(Ok(s"async raw with request $request"))
  }

}
