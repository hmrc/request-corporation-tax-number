/*
 * Copyright 2023 HM Revenue & Customs
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

package templates

import helper.TestFixture
import model.CompanyDetails
import model.templates.SubmissionViewModel
import org.jsoup.Jsoup
import play.twirl.api.Html

class CTUTRSchemeSpec extends TestFixture {

  private val submitValidViewModel: SubmissionViewModel = SubmissionViewModel(
    company = CompanyDetails(
      companyName = "Big company",
      companyReferenceNumber = "AB123123"
    ),
    timeStamp = "Tuesday 31 October 2017 at 15:18:12"
  )

  private def createSUT(viewModel: SubmissionViewModel): Html = templates.html.CTUTRScheme(viewModel)

  "CTUTR Form" should {

    "display the correct sections" when {

      "an submissionViewModel is provided with valid data" in {
        val sut = createSUT(submitValidViewModel)

        val doc = Jsoup.parse(sut.toString())

        val companyName = doc.getElementById("companyDetails").select("tbody > tr:nth-of-type(1)")
        val companyReference = doc.getElementById("companyDetails").select("tbody > tr:nth-of-type(2)")

        companyName.select("td:nth-of-type(1)").text() mustBe "Company name"
        companyName.select("td:nth-of-type(2)").text() mustBe "Big company"

        companyReference.select("td:nth-of-type(1)").text() mustBe "Company reference"
        companyReference.select("td:nth-of-type(2)").text() mustBe "AB123123"
      }
    }
  }
}
