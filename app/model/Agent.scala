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

import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

case class Agent(name: String,
                 ukAddress: Option[UkAddress],
                 internationalAddress: Option[InternationalAddress],
                 telephoneNumber: Option[String],
                 emailAddress: Option[String])

object Agent {

  implicit val reads : Reads[Agent] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "ukAddress").readNullable[UkAddress] and
      (JsPath \ "internationalAddress").readNullable[InternationalAddress] and
      (JsPath \ "telephoneNumber").readNullable[String] and
      (JsPath \ "emailAddress").readNullable[String]
    )(Agent.apply _)

  implicit val writes : Writes[Agent] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "ukAddress").writeNullable[UkAddress] and
      (JsPath \ "internationalAddress").writeNullable[InternationalAddress] and
      (JsPath \ "telephoneNumber").writeNullable[String] and
      (JsPath \ "emailAddress").writeNullable[String]
    )(unlift(Agent.unapply))

}