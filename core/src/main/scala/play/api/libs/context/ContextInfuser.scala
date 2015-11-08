package play.api.libs.context

import scala.annotation.implicitNotFound

/**
  * Infuses the context the the target.
  *
  * @note this is called an "infuser", because while in many cases it can and should be possible to extract
  *       the context back out from the infused result, it is not a requirement. This is so that this can
  *       represent things that might be more like a one-way hashing function.
  *
  * This is counter-part to the [[ContextExtractor]].
  *
  * @tparam Ctx the type of context to infuse into the result
  * @tparam Target the input to apply the context to
  * @tparam Infused the infused result
  */
@implicitNotFound("No implicit WSRequestInfuser[${Ctx}] found. " +
  "The infuser is needed to apply the values of the ${Ctx} to the ${Target} " +
  "in order to produce the correct outgoing ${Infused}.")
trait ContextInfuser[-Ctx, -Target, +Infused] {

  /**
    * Infuse the target with the implicit context.
    */
  def infuse(target: Target)(implicit context: Ctx): Infused
}
