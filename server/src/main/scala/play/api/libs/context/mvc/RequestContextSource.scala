package play.api.libs.context.mvc

/**
  * The source of the context that is extracted from the [[play.mvc.Http.Request]].
  */
sealed trait RequestContextSource {
  def name: String
}

/**
  * A single source for the context.
  */
sealed trait SingleContextSource extends RequestContextSource

/**
  * The context comes from the headers.
  */
case object ContextFromHeaders extends SingleContextSource {
  override def name: String = "headers"
}

/**
  * The context comes from the query params.
  */
case object ContextFromQueryParams extends SingleContextSource {
  override def name: String = "query params"
}

/**
  * The context comes multiple sources.
  */
case class RequestContextSources(sources: Seq[SingleContextSource]) extends RequestContextSource {
  override def name: String = sources.mkString(", ")
}
