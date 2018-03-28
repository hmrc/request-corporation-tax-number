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

case class Submission(company: CompanyDetails) {

  val time : LocalDateTime = {
    val zone = DateTimeZone.forID("Europe/London")
    LocalDateTime.now(zone)
  }

}

object Submission {

  //  implicit val reads : Reads[Submission] = (JsPath \ "company").read[CompanyDetails](Submission.apply _)
  implicit val reads : Reads[Submission] = (JsPath \ "company").read[CompanyDetails].map(Submission(_))

  //implicit val writes : Writes[Submission] = (JsPath \ "company").write[CompanyDetails](unlift(Submission.unapply))
  implicit val writes : Writes[Submission] = (JsPath \ "company").write[CompanyDetails].contramap(unlift(Submission.unapply))

}