/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class InternationalAddress(addressLine1: String,
                                addressLine2: String,
                                addressLine3: Option[String],
                                addressLine4: Option[String],
                                addressLine5: Option[String],
                                country: String)

object InternationalAddress {

  implicit val reads : Reads[InternationalAddress] = (
    (JsPath \ "addressLine1").read[String] and
    (JsPath \ "addressLine2").read[String] and
    (JsPath \ "addressLine3").readNullable[String] and
    (JsPath \ "addressLine4").readNullable[String] and
    (JsPath \ "addressLine5").readNullable[String] and
    (JsPath \ "country").read[String]
    )(InternationalAddress.apply _)

  implicit val write : Writes[InternationalAddress] = (
    (JsPath \ "addressLine1").write[String] and
      (JsPath \ "addressLine2").write[String] and
      (JsPath \ "addressLine3").writeNullable[String] and
      (JsPath \ "addressLine4").writeNullable[String] and
      (JsPath \ "addressLine5").writeNullable[String] and
      (JsPath \ "country").write[String]
    )(unlift(InternationalAddress.unapply))

}