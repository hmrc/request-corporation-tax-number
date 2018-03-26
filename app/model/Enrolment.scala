package model

import org.joda.time.{DateTimeZone, LocalDateTime}
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class Enrolment(capacityRegistering: String,
                     employer: Employer,
                     agent: Option[Agent]) {

  val time : LocalDateTime = {
    val zone = DateTimeZone.forID("Europe/London")
    LocalDateTime.now(zone)
  }

}

object Enrolment {

  implicit val reads : Reads[Enrolment] = (
  (JsPath \ "capacityRegistering").read[String] and
    (JsPath \ "employer").read[Employer] and
    (JsPath \ "agent").readNullable[Agent]
  )(Enrolment.apply _)

  implicit val writes : Writes[Enrolment] = (
    (JsPath \ "capacityRegistering").write[String] and
      (JsPath \ "employer").write[Employer] and
      (JsPath \ "agent").writeNullable[Agent]
    )(unlift(Enrolment.unapply))

}