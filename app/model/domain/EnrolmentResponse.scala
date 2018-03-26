package model.domain

import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class EnrolmentResponse(id: String, filename : String)

object EnrolmentResponse {

  implicit val writes : Writes[EnrolmentResponse] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "filename").write[String]
    )(unlift(EnrolmentResponse.unapply))

  implicit val reads : Reads[EnrolmentResponse] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "filename").read[String]
  )(EnrolmentResponse.apply _)

}
