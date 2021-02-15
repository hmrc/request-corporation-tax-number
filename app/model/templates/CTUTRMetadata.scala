/*
 * Copyright 2021 HM Revenue & Customs
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
import org.joda.time.LocalDateTime
import utils.SubmissionReferenceHelper.createSubmissionRef

case class CTUTRMetadata(appConfig : MicroserviceAppConfig,
                        customerId: String = "") {

  val hmrcReceivedAt : LocalDateTime = LocalDateTime.now()
  val xmlCreatedAt: LocalDateTime = LocalDateTime.now()
  val submissionReference: String = createSubmissionRef()

  val reconciliationId: String = s"$submissionReference-" + xmlCreatedAt.toString("yyyyMMddHHmmss")
  val fileFormat: String = "pdf"
  val mimeType: String = "application/pdf"

  val casKey : String = ""
  val submissionMark : String = ""
  val attachmentCount : Int = 0
  val numberOfPages : Int = 2

  lazy val formId : String = appConfig.CTUTR.formId
  lazy val businessArea : String = appConfig.CTUTR.businessArea
  lazy val classificationType : String = appConfig.CTUTR.queue
  lazy val source : String = appConfig.CTUTR.source
  lazy val target : String = appConfig.CTUTR.target
  lazy val store : Boolean = appConfig.CTUTR.save
}