/*
 * Copyright 2025 HM Revenue & Customs
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

import model.templates.CTUTRMetadata
import play.api.libs.json.{Format, Json}

import java.time.LocalDateTime

case class MongoSubmission(
                            companyName: String,
                            companyReferenceNumber: String,
                            time: LocalDateTime,
                            submissionReference: String,
                            customerId: String = ""
                          )

object MongoSubmission {
  implicit val formats: Format[MongoSubmission] = Json.format[MongoSubmission]

  def apply(submission: Submission,
            metadata: CTUTRMetadata): MongoSubmission = new MongoSubmission(
    submission.companyDetails.companyName,
    submission.companyDetails.companyReferenceNumber,
    metadata.metadataCreatedAt,
    metadata.submissionReference,
    metadata.customerId
  )
}
