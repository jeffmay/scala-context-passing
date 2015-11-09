package example.cache

import example.models.{AuthSession, User}

import scala.concurrent.Future
import scala.language.implicitConversions

trait UserClient {

  def fetchUser(implicit authInfo: AuthSession): Future[User]
}
