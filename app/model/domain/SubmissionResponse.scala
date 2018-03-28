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

package model.domain

import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class SubmissionResponse(id: String, filename : String)

object SubmissionResponse {

  implicit val writes : Writes[SubmissionResponse] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "filename").write[String]
    )(unlift(SubmissionResponse.unapply))

  implicit val reads : Reads[SubmissionResponse] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "filename").read[String]
  )(SubmissionResponse.apply _)

}