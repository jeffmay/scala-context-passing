package play.api.libs.context

/**
  * An object that provides access to helper methods for everything related to generic context manipulation.
  *
  * It provides helper methods to infuse a context with a source and to extract a context from a target.
  *
  * A "context" is an object at runtime that provides a type-safe mechanism for requiring common configuration
  * throughout an application.
  *
  * A good use case for context is for authorization. By adding an implicit parameter that requires a certain
  * level of authorized context, the programmer can insure at compile-time that the context is parsed and passed
  * along correctly without the use of thread-locals or other global singleton-like magic. Every call along
  * the way explicitly accepts this context without requiring the caller to explicitly pass it. It puts a
  * little more burden on the library writer, but insures that the library user has full understanding of
  * all of the dependencies of invoking a method at compile-time.
  */
object Context {

  /**
    * Pulls the infuser from implicit scope.
    */
  def infuser[Ctx, In, Out](implicit infuser: ContextInfuser[Ctx, In, Out]): ContextInfuser[Ctx, In, Out] = infuser

  /**
    * Uses the implicit infuser and context in scope to infuse the given input to produce the infused output.
    */
  def infuse[Ctx, In, Out](input: In)(implicit context: Ctx, infuser: ContextInfuser[Ctx, In, Out]): Out = {
    infuser.infuse(input)(context)
  }

  /**
    * Creates an intermediary [[FromSourceBuilder]] for building the final [[ContextExtractor]].
    */
  def from[Source](source: Source): FromSourceBuilder[Source] = new FromSourceBuilder(source)

  /**
    * An immutable builder for better syntax without encountering type erasure issues with overloaded methods.
    */
  class FromSourceBuilder[S](source: S) {

    /**
      * Uses an implicit extractor to produce the expected source or throw an exception.
      *
      * @note this will accept any context extractor regardless of the error type supported by the extractor.
      */
    def extractOrThrow[Ctx](implicit extractor: ContextExtractor[S, Ctx, _]): Ctx = {
      extractor.extractOrThrow(source)
    }
  }
}

