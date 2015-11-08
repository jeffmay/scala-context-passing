package play.api.libs.context.mvc

import org.scalactic.{Bad, Good, Or}
import play.api.mvc._

import scala.concurrent.Future
import scala.language.reflectiveCalls

/**
  * An action builder (similar to [[ActionWithoutContext]]), but additionally has methods to create
  * [[ActionWithContext]] action builders.
  *
  * In order to create an action that requires an implicit context, you have to specify a type
  * and a [[ReadsRequestContext]] to extract it from the request with.
  */
object ActionWithContext extends ActionWithoutContext {

  /**
    * Returns an action builder that is able to build controller with functions that use an implicit context.
    */
  def fromHeaders[Ctx](implicit readsHeader: ReadsRequestContext.FromHeaders[Ctx]): ActionWithContext[Ctx] = {
    new ActionWithContext[Ctx] {
      override def readContext[A](request: Request[A]): Ctx Or Result = readsHeader.extractOrResponse(request)
    }
  }
}

/**
  * An action builder (similar to Play's [[Action]] but with different syntax) that provides a
  * familiar yet descriptive mechanism to build [[Controller]] actions.
  */
trait ActionWithoutContext {

  // Same as Action.apply
  def syncRaw(result: => Result): Action[AnyContent] = Action(result)
  def syncRaw(block: Request[AnyContent] => Result): Action[AnyContent] = Action(block)
  def syncRaw[A](bodyParser: BodyParser[A])(block: Request[AnyContent] => Result): Action[AnyContent] =
    Action(block)

  // Same as Action.async
  def asyncRaw(result: => Future[Result]): Action[AnyContent] = Action.async(result)
  def asyncRaw(block: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async(block)
  def asyncRaw[A](bodyParser: BodyParser[A])(block: Request[AnyContent] => Future[Result]): Action[AnyContent] =
    Action.async(block)
}

/**
  * An action builder (similar to [[ActionWithoutContext]]), but has accepts functions that require an
  * implicit context.
  *
  * @note That funky little type-lambda allows me to carry the [[Ctx]] type parameter into a higher-kinded
  *       type to satisfy the ActionFunction parameter, but without giving up the specificity of the
  *       context type required to invoke this block and instead creating an anonymous type "Body" to
  *       stuff the invokeBlock type parameter into.
  *
  * @tparam Ctx The type of context to parse from the request
  */
trait ActionWithContext[Ctx]
  extends ActionWithoutContext
  with ActionFunction[Request, ({type λ[Body] = (Ctx, Request[Body])})#λ] {

  /**
    * Read the context from the request or responds immediately.
    *
    * @note this is a good place to invoke [[ReadsRequestContext.extractOrResponse]]
    */
  def readContext[A](request: Request[A]): Ctx Or Result

  /**
    * Execute asynchronously with the context and request as arguments that can both be made implicit
    */
  def async(doAsync: Ctx => Request[AnyContent] => Future[Result]): Action[AnyContent] = {
    async(BodyParsers.parse.default)(doAsync)
  }

  /**
    * Execute asynchronously after applying the body parser with the context and request as arguments
    * that can both be made implicit
    */
  def async[A](bodyParser: BodyParser[A])(doAsync: Ctx => Request[A] => Future[Result]): Action[A] = {
    Action.async(bodyParser) { request =>
      readContext(request) match {
        case Good(ctx)   => doAsync(ctx)(request)
        case Bad(result) => Future.successful(result)
      }
    }
  }

  /**
    * Execute synchronously with the context and request as arguments that can both be made implicit
    */
  def sync(doSync: Ctx => Request[AnyContent] => Result): Action[AnyContent] = {
    sync(BodyParsers.parse.default)(doSync)
  }

  /**
    * Execute synchronously after applying the body parser with the context and request as arguments
    * that can both be made implicit
    */
  def sync[A](bodyParser: BodyParser[A])(doSync: Ctx => Request[A] => Result): Action[A] = {
    async(bodyParser)(doSync.andThen(_.andThen(Future.successful)))
  }

  /**
    * Invoke the block.  This is the main method that an ActionBuilder has to implement, at this stage it can wrap it in
    * any other actions, modify the request object or potentially use a different class to represent the request.
    *
    * @param request The request
    * @param block The block of code to invoke with both the context and the request.
    * @return A future of the result
    */
  override def invokeBlock[A](request: Request[A], block: ((Ctx, Request[A])) => Future[Result]): Future[Result] = {
    readContext(request) match {
      case Good(ctx) => block((ctx, request))
      case Bad(result) => Future.successful(result)
    }
  }
}

