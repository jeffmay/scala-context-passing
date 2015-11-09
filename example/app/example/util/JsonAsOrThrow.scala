package example.util

import play.api.libs.json._

import scala.language.implicitConversions
import scala.reflect.ClassTag

// TODO: Document
trait JsonAsOrThrow {

  // TODO: Add this to play-json-ops
  implicit def asOrThrowJsError[T](json: JsValue): JsValueOrThrow = new JsValueOrThrow(json)
}

// TODO: Document
class JsValueOrThrow(val json: JsValue) extends AnyVal {

  def asOrThrow[T](implicit classTag: ClassTag[T], reads: Reads[T]): T = json.validate[T] match {
    case JsSuccess(value, _) => value
    case errors: JsError => throw new InvalidJsonException[T](errors)
  }
}

// TODO: Document
case class InvalidJsonException[T](errors: JsError)(implicit tag: ClassTag[T])
  extends RuntimeException({
    val className = tag.runtimeClass.getName
    val jsonErrors = JsError.toJson(errors)
    s"Could not read an instance of $className, encountered parse errors: ${Json.prettyPrint(jsonErrors)}"
  }) {

  def this(resultException: JsResultException)(implicit tag: ClassTag[T]) = this(JsError(resultException.errors))
}
