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

case class SubmissionViewModel(company: CompanyDetails, timeStamp: String)

object SubmissionViewModel {

  private def modelCompanyDetails(company: model.CompanyDetails) : CompanyDetails = {
    CompanyDetails (
      companyName = company.companyName,
      companyReferenceNumber = company.companyReferenceNumber
    )
  }

  def apply(submission : Submission) : SubmissionViewModel = {

    val timestamp = s"${submission.time.toString("EEEE dd MMMM yyyy")} at ${submission.time.toString("HH:mm:ss")}"

    SubmissionViewModel(
      company = modelCompanyDetails(submission.companyDetails),
      timeStamp = timestamp
    )
  }

}
