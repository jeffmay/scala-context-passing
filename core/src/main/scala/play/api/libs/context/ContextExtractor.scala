package play.api.libs.context

import org.scalactic.{Bad, Good, Or}
import play.api.libs.context.functional.Show
import play.api.libs.json.Writes

import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

/**
  * Used to extract context from a source.
  *
  * This is counter-part to the [[ContextInfuser]].
  *
  * @tparam Source the type of source from which to extract the context
  * @tparam Ctx the type of context to extract
  * @tparam Errors the type of error(s) that can be returned instead. Note, this is plural
  *                because it is rare to go from unstructured data to structured data without
  *                needing the ability to accumulate all the errors.
  *                I strongly encourage the [[org.scalactic.Every]] type here to wrap your errors.
  */
@implicitNotFound("No implicit ContextExtractor for ${Ctx} found. " +
  "The extractor is needed to retrieve from the ${Source} either a ${Ctx} or ${Errors}.")
trait ContextExtractor[-Source, +Ctx, Errors] {

  /**
    * A means to display the errors type.
    *
    * This is required because errors that can't be shown are kind of useless and relying
    * on .toString is dangerous and threatens the quality of intentional error messaging.
    */
  implicit def showErrors: Show[Errors]

  /**
    * Safely extract the source into the context or some errors.
    *
    * @note while it would be bad practice for this method to throw an exception,
    *       there is nothing to prevent it.
    *
    * @param source the source from which to extract the context.
    */
  def extractOrErrors(source: Source): Ctx Or Errors

  /**
    * Attempt to extract the context from the source or throw an exception.
    *
    * @param source the source from which to extract the context.
    */
  def extractOrThrow(source: Source): Ctx
}

object ContextExtractor {

  /**
    * A simple [[ContextExtractor]] that only ever throws exceptions.
    *
    * @note this is typically a bad way to structure your extractors, but it is useful if you only ever
    *       care to throw an exception for this type of context or source.
    *
    * @tparam Source the source from which to extract the context.
    * @tparam Ctx the context to extract from the source.
    */
  type OrThrows[-Source, +Ctx] = ContextExtractor[Source, Ctx, Nothing]

  /**
    * Creates an intermediary [[FromSourceBuilder]] for building the final [[ContextExtractor]].
    */
  def from[S]: FromSourceBuilder[S] = new FromSourceBuilder

  /**
    * An immutable builder for better syntax without encountering type erasure issues with overloaded methods.
    */
  class FromSourceBuilder[Source] {

    /**
      * Builds a [[ContextExtractor.OrThrows]] from the given function.
      *
      * @param extractor a function that either extracts a context or throws an exception.
      */
    def orThrow[Ctx](extractor: Source => Ctx): ContextExtractor.OrThrows[Source, Ctx] = {
      new ContextExtractorOrThrows(extractor)
    }

    /**
      * Builds a [[ContextExtractor]] from the given function.
      *
      * @param extractor a function that either extracts a context or some errors.
      */
    def orErrors[Ctx: ClassTag, Errors: Writes: Show](
      extractor: Source => Ctx Or Errors): ContextExtractor[Source, Ctx, Errors] = {
      new ContextExtractorOrErrors[Source, Ctx, Errors](extractor)
    }
  }
}

/**
  * A default [[ContextExtractor]] that throws exceptions on every method that attempts to extract a context
  * rather than returning any error values.
  *
  * @note throwing exceptions is generally bad Scala, however, there are times where that is all you would
  *       want to do, and this allows you to define a general purpose exception throwing extractor for
  *       library users who want to extract a context without any concern for handling the error case.
  *
  * @param extractor a function that extracts the expected context or throws an exception
  */
class ContextExtractorOrThrows[-Source, +Ctx](extractor: Source => Ctx)
  extends ContextExtractor[Source, Ctx, Nothing] {

  override implicit def showErrors: Show[Nothing] = ???  // No way to show a thrown exception

  override def extractOrErrors(source: Source): Ctx Or Nothing = Good(extractor(source))

  override def extractOrThrow(source: Source): Ctx = extractor(source)
}

/**
  * A default [[ContextExtractor]] that returns the expected context or errors type and handles the case
  * when the caller prefers to get an exception.
  *
  * @param extractor a function that extracts the expected context or errors object
  */
class ContextExtractorOrErrors[-Source, +Ctx, Errors](extractor: Source => Ctx Or Errors)
  (implicit contextTag: ClassTag[Ctx], writesErrors: Writes[Errors], showsErrors: Show[Errors])
  extends ContextExtractor[Source, Ctx, Errors] {

  override implicit def showErrors: Show[Errors] = showsErrors

  override def extractOrErrors(source: Source): Ctx Or Errors = extractor(source)

  override def extractOrThrow(source: Source): Ctx = extractor(source) match {
    case Good(context) => context
    case Bad(errors) =>
      val contextClassName = contextTag.runtimeClass.getName
      throw new DefaultContextExtractException[Errors](s"Could not extract an instance of $contextClassName", errors)
  }
}
