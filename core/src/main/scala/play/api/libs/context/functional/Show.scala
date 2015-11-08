package play.api.libs.context.functional

import play.api.libs.functional.ContravariantFunctor

/**
  * A type class to provide textual representation. It is meant to be a
  * better "toString". Whereas toString exists for any Object,
  * regardless of whether or not the creator of the class explicitly
  * made a toString method, a Show instance will only exist if someone
  * explicitly provided one.
  *
  * @note This is copied from the Cats project. Once it is more stable,
  *       this will be replaced.
  */
trait Show[T] extends Serializable {
  def show(f: T): String
}

object Show {

  /**
    * Used to show the given value with the implicit Show in scope.
    */
  def asString[A](a: A)(implicit s: Show[A]): String = s.show(a)

  /** creates an instance of [[Show]] using the provided function */
  def show[A](f: A => String): Show[A] = new Show[A] {
    def show(a: A): String = f(a)
  }

  /** creates an instance of [[Show]] using object toString */
  def fromToString[A]: Show[A] = new Show[A] {
    def show(a: A): String = a.toString
  }

  /**
    * Allows you to using .contramap from [[play.api.libs.functional.syntax]].
    */
  implicit val showContravariant: ContravariantFunctor[Show] = new ContravariantFunctor[Show] {
    def contramap[A, B](fa: Show[A], f: B => A): Show[B] =
      show[B](fa.show _ compose f)
  }
}