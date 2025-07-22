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

import model.{CompanyDetails, Submission}

import java.time.format.DateTimeFormatter

case class SubmissionViewModel(company: CompanyDetails, timeStamp: String)

object SubmissionViewModel {

  def apply(submission: Submission, metadata: CTUTRMetadata) : SubmissionViewModel = {

    val timestamp = s"${metadata.createdAt.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"))}" +
      s" at ${metadata.createdAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"

    SubmissionViewModel(
      company = submission.companyDetails,
      timeStamp = timestamp
    )
  }

}
