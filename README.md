# Scala Context Passing

This library enables some type-safe and stateless patterns for supporting passing implicit context.

# The Problem

You want something akin to a `ThreadLocal` variable, but you use async patterns such as `Future`s and
stateless functional programming patterns.

# The Solution

Keep using stateless and functional programming patterns. You just need some syntactic sugar and a
good foundation for these patterns that are specific to your needs and well documented. This library
attempts to provide a different way of thinking about "global" state by encouraging you to defer the
use of state until you absolutely need to use it.

This library adds the notion of a generic `ContextInfuser` and a `ContextExtractor`. These interfaces
are defined by default for some Play projects including `play-server` and `play-ws`, however the
common `context-core` library could be conceivably be adapted for other projects whilst sharing
common generic infusers and extractors.

# Usage

One good use case for this project is for authorization. Your service wants to make requests to another
server on behalf of a user. You can either pass the authentication / authorization values explicitly
to the clients that will issue the requests or you could use an implicit context.

Here is how it would look without an implicit context:

```scala
case class AuthSession(authToken: String)

class UserClient(ws: WSClient) {
  def viewUser(auth: AuthSession, otherUserId: String): Future[User] = {
    ws.url(s"/rest/profile/$otherUserId").withHeaders("X-AuthToken" -> auth.authToken).get().map {
      resp => resp.json.as[User]
    }
  }
}
```

The problem with this is two-fold.

1. Boilerplate for adding the headers
2. Boilerplate on the caller to provide the specific authentication type 
   
This is problematic because if you decide to change the authentication implementation even just a little
bit, it becomes a major refactor that is akin to tearing our one's own heart and stapling on a new one.

Scala has so much power in the type-system to alleviate this pain. Let's try to add some sugar here:

```scala
trait AuthSession {
  def authToken: String
}

trait UserSession extends AuthSession {
  def userId: String
}

case class UserSessionImpl(authToken: String, userId: String) extends UserSession

class UserClient(ws: WSClient) {
  def viewUser(otherUserId: String)
  (implicit auth: AuthSession with UserSession, infuser: UserSession => WSRequest => WSRequest): Future[User] = {
    infuser(auth)(ws.url(s"/rest/profile/$otherUserId")).get().map {
      resp => resp.json.as[User]
    }
  }
}
```

Ok, so we've improved the situation in three ways and made it worse in one way.

**Good:**

1. We've separated the trait from the case class so that we can support different contexts with mixins
2. We've made the required context implicit to avoid the caller needing to pass it explicitly
3. We've made the function that adds the headers to the request implicit

All three of these cut down on the coupling and make the code easier to refactor at the cost of one thing:

**Bad:**

1. A lot of boilerplate

Okay, so we can fix this. We just need some helpful constructs. What if I told you it could look like:
   
```scala
class UserClient(authWs: MinimumContextWSClient[UserSession]) {
  def viewUser(otherUserId: String)(implicit auth: UserSession): Future[User] = {
    authWs.url(s"/rest/profile/$otherUserId").withContext.get().map {
      resp => resp.json.as[User]
    }
  }
}
```

**Sweet!** That is the purpose of this library.

# Play WS Support

The Play WS library (`context-play-ws`) enables the following tools:
 
- `ContextPassingWSClient`: Replaces `WSClient` to enable requesting an implicit context to issue a request
  such that an implicit `ContextInfuser` for `WSRequest`s is provided.
- `MinimumContextWSClient`: Replaces `WSClient` to require passing an implicit context of at least the type
  specified in the type parameter. This also enables type-wise dependency injection, but the type can be
  ignored by using `.withoutContextType` to get an untyped `ContextPassingWSClient`
- `WSRequestWithContext`: Returned by both the above clients that infuses the implicit context implicitly.
- `WSRequestInfuser`: An object that makes building infusers for `WSRequest` simpler by providing some 
  common infuser techniques such as adding headers to a request.
   
**TODO:**

- Support for infusing context with query params
- Support for infusing context with the body of the request (low priority as this is generally bad practice, 
  but it could be useful for someone, so I'll just point it out)

# Play Server Support

The Play Server library (`context-play-server`) enables the following tools:

- `ActionWithContext`: Replaces `Action` to provide additional methods for building an action with a function
  that is injected with a context in such a way that it is easy to make this implicit.
- `ReadsRequestContext`: A subclass of `ContextExtractor` that is specific to `play.api.mvc.Request`s. This
  provides some common extraction techniques, such as reading from headers.
- `HeaderReader`: A helpful wrapper around reading values from a request and accumulating errors from missing 
  headers. This is used in conjunction with `HeaderError` which is a special class that captures common errors
  when parsing and validating values from the headers.

**TODO:**

- Support for extracting context from cookies
- Support for extracting context from the Play session
- Support for extracting context from query params (maybe sharing a common error type with `HeaderError`)
- Support for extracting context from the routes file (maybe using a custom `PathBindable`?)
- Support for extracting context from the body of the request (low priority as this is generally bad practice, 
  but it could be useful for someone, so I'll just point it out)

# Example Play Project

This repo includes an example Play project for demoing off some of the features.

Checkout: `example/app/example` (TODO: Figure out how to link this better)
