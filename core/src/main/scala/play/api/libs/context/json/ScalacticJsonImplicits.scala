package play.api.libs.context.json

import org.scalactic.{Many, One, Every}
import play.api.libs.json.{JsError, JsArray, Reads, Writes}

trait ScalacticJsonImplicits {

  implicit def writesEvery[T: Writes]: Writes[Every[T]] = {
    Writes(every => Writes.of[Seq[T]].writes(every.toSeq))
  }

  implicit def readsEvery[T: Reads]: Reads[Every[T]] = {
    val readsSeq = Reads.of[Seq[T]]
    Reads {
      case JsArray(Seq()) => JsError("Every cannot be empty")
      case other => readsSeq.reads(other).map(seq => Every(seq.head, seq.tail: _*))
    }
  }

  implicit def readsOne[T: Reads]: Reads[One[T]] = Reads.of[T].map(One(_))

  implicit def readsMany[T: Reads]: Reads[Many[T]] = {
    val readsSeq = Reads.of[Seq[T]]
    Reads {
      case JsArray(Seq()) => JsError("Many cannot be empty")
      case JsArray(Seq(one)) => JsError("Many must contain more than one element")
      case other => readsSeq.reads(other).map(seq => Many(seq.head, seq.tail.head, seq.tail.tail: _*))
    }
  }
}
