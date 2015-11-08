package play.api.libs.context.ws

import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar
import play.api.libs.ws.WSRequest

case class FakeContext(value: String)
object FakeContext {
  val Header = "header"
  implicit val infuser = WSRequestInfuser[FakeContext] { ctx =>
    _.withHeaders(Header -> ctx.value)
  }
}

class WSRequestWithContextSpec extends WordSpec with MockitoSugar {

  "WSRequestWithContext" should {

    "carry the implicit context" in {
      implicit val context = FakeContext("unused context")
      val request = WSRequestWithContext[FakeContext](mock[WSRequest])
      assert(request.context === context)
    }

    "support the withContext syntax" in {
      implicit val context = FakeContext("unused context")
      val request = mock[WSRequest]
      assert(request.withContext[FakeContext].context === context)
    }

    "not allow calling withContext on a request with context" in {
      implicit val context = FakeContext("unused context")
      val request = WSRequestWithContext[FakeContext](mock[WSRequest])
      assertDoesNotCompile("request.withContext[FakeContext]")
    }

    "apply the context after updating the request" in {
      implicit val context = FakeContext("applied context")
      val mockRequest = FakeWSRequest()
      val request = WSRequestWithContext[FakeContext](mockRequest)
      val beforeContext = "before context"
      val withHeaders = request.withHeaders(FakeContext.Header -> beforeContext)
      val actual = withHeaders.withContext.headers(FakeContext.Header)
      assert(actual === Seq(beforeContext, context.value))
    }
  }
}
