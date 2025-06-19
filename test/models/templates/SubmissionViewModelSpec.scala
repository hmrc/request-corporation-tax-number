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

package models.templates

import helper.TestFixture
import model.MongoSubmission
import model.templates.SubmissionViewModel

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubmissionViewModelSpec extends TestFixture {

  "SubmissionViewModel" must {

    "instantiate when provided an submission with valid data" in {

      val mongoSubmission = new MongoSubmission(
        companyName = "Big Company",
        companyReferenceNumber = "AB123123",
        time = LocalDateTime.parse("Tuesday 31 October 2017 15:18:12", DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy HH:mm:ss")),
        submissionReference = ""
      )

      SubmissionViewModel.apply(mongoSubmission) mustBe SubmissionViewModel(
        company = model.CompanyDetails(
          companyName = "Big Company",
          companyReferenceNumber = "AB123123"
        ),
        timeStamp = "Tuesday 31 October 2017 at 15:18:12"
        )
    }
  }

}
