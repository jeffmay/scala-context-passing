package example.models

import java.util.UUID

import example.context.{HeaderConstants, HeaderKeys}
import org.scalactic.{Good, Bad}
import play.api.libs.context.mvc.ReadsRequestContext
import play.api.libs.context.ws.WSRequestInfuser
import play.api.mvc.Results
import org.scalactic.Accumulation._

sealed trait AnySession {
  def sessionId: String
}
object AnySession {

  implicit val addToHeaders = WSRequestInfuser.addToHeaders[AnySession] { ctx =>
    var headers = Seq(
      HeaderKeys.SessionIdHeader -> ctx.sessionId
    )
    ctx match {
      case auth: AuthSession =>
        headers :+= HeaderKeys.UserIdHeader -> auth.userId
      case other =>
    }
    headers
  }

  implicit val fromHeaders = ReadsRequestContext.chooseFromHeader(HeaderKeys.UserIdHeader) {
    case Some(userId) => AuthSession.fromHeaders
    case None => UnAuthSession.fromHeaders
  } orResult Results.Unauthorized
}

case class UnAuthSession(sessionId: String) extends AnySession {
  final def apiUserId: String = HeaderConstants.ApiUnAuthUserId
}
object UnAuthSession {
  implicit val fromHeaders = ReadsRequestContext.usingHeaders[UnAuthSession] { headers =>
    withGood(headers.get(HeaderKeys.SessionIdHeader)) { sessionId =>
      UnAuthSession(sessionId)
    }
  } orResult Results.Unauthorized
}

case class AuthSession(userId: String, sessionId: String) extends AnySession
object AuthSession {
  implicit val fromHeaders = ReadsRequestContext.usingHeaders[AuthSession] { headers =>
    withGood(
      headers.get(HeaderKeys.UserIdHeader),
      headers.getAs[UUID](HeaderKeys.SessionIdHeader)
    ) {
      (userId, sessionId) =>
        AuthSession(userId, sessionId.toString)
    }
  } orResult Results.Unauthorized
}
