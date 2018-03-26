/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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