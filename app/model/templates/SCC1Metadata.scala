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

package model.templates

import config.MicroserviceAppConfig
import org.joda.time.LocalDateTime

case class SCC1Metadata(appConfig : MicroserviceAppConfig,
                        customerId: String = "") {

  val hmrcReceivedAt : LocalDateTime = LocalDateTime.now()
  val xmlCreatedAt: LocalDateTime = LocalDateTime.now()
  val submissionReference: String = xmlCreatedAt.toString("ssMMyyddmmHH")
  val reconciliationId: String = submissionReference
  val fileFormat: String = "pdf"
  val mimeType: String = "application/pdf"

  // sent to cas service which generates a 3-4-3 key in their service. encoded version of the pdf payload. Becomes the reconciliation id and the title
  val casKey : String = ""
  val submissionMark : String = ""
  val attachmentCount : Int = 0
  val numberOfPages : Int = 2

  lazy val formId : String = appConfig.SCC1.formId
  lazy val businessArea : String = appConfig.SCC1.businessArea
  lazy val classificationType : String = appConfig.SCC1.queue
  lazy val source : String = appConfig.SCC1.source
  lazy val target : String = appConfig.SCC1.target
  lazy val store : Boolean = appConfig.SCC1.save
}