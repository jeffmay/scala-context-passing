package play.api.libs.context

import scala.language.implicitConversions

/**
  * Import from this package to give you implicit [[play.api.libs.context.functional.Show]] instances
  * for Json values.
  *
  * This is useful for defining ways to show errors for [[ContextExtractor]]s.
  */
package object json extends ShowJsonSyntax with ScalacticJsonImplicits
