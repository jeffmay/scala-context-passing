package example.models

import play.api.libs.json.{Format, Json}

case class User(id: String, displayName: String, fullName: String)
object User {
  implicit val format: Format[User] = Json.format[User]
}