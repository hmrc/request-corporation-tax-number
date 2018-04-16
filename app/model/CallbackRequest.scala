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

import play.api.libs.json.{Format, Json}

/**
  * TODO implement toMap method when auditing to splunk
  */
case class CallbackRequest(envelopeId: String, fileId: String, status: String, reason: Option[String] = None) {
//  def toMap: Map[String, String] = Map(
//    "envelopId" -> envelopeId,
//    "status" -> status,
//    "reason" -> reason.getOrElse(""),
//    "file-Id" -> fileId
//  )
}

object CallbackRequest {
  implicit val formatFileUpload: Format[CallbackRequest] = Json.format[CallbackRequest]
}


