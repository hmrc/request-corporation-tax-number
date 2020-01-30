/*
 * Copyright 2020 HM Revenue & Customs
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

import model.templates.SubmissionViewModel
import model.Submission
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class SubmissionViewModelSpec extends PlaySpec with MockitoSugar {

  val time = LocalDateTime.parse("Tuesday 31 October 2017 15:18:12", DateTimeFormat.forPattern("EEEE dd MMMM yyyy HH:mm:ss"))

  "SubmissionViewModel" must {

    "instantiate when provided an submission with valid data" in {
      val submission = mock[Submission]
      when(submission.companyDetails).thenReturn(model.CompanyDetails(
        companyName = "Big Company",
        companyReferenceNumber = "AB123123"
      ))
      when(submission.time).thenReturn(time)

      SubmissionViewModel.apply(submission) mustBe SubmissionViewModel(
        company = model.CompanyDetails(
          companyName = "Big Company",
          companyReferenceNumber = "AB123123"
        ),
        timeStamp = "Tuesday 31 October 2017 at 15:18:12"
        )
    }
  }

}
