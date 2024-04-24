/*
 * Copyright 2024 HM Revenue & Customs
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

package model.templates

import config.MicroserviceAppConfig
import utils.SubmissionReferenceHelper.createSubmissionRef

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class CTUTRMetadata(appConfig : MicroserviceAppConfig, customerId: String = "") {

  val xmlCreatedAt: String = now("dd/MM/yyyy HH:mm:ss")
  val hmrcReceivedAt : String = now("dd/MM/yyyy HH:mm:ss")
  val submissionReference: String = createSubmissionRef()

  val reconciliationId: String = s"$submissionReference-" + now("yyyyMMddHHmmss")
  val fileFormat: String = "pdf"
  val mimeType: String = "application/pdf"

  val casKey : String = ""
  val submissionMark : String = ""
  val attachmentCount : Int = 0
  val numberOfPages : Int = 2

  lazy val formId : String = appConfig.formId
  lazy val businessArea : String = appConfig.businessArea
  lazy val classificationType : String = appConfig.queue
  lazy val source : String = appConfig.source
  lazy val target : String = appConfig.target
  lazy val store : Boolean = appConfig.save

  private def now(dateTimePattern:String) = LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateTimePattern))

}
