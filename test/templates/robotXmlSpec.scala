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

package templates

import config.SpecBase
import model.CompanyDetails
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import play.twirl.api.Xml

class robotXmlSpec extends SpecBase {

  "robotXml" should {

    "not have a line feed character at the top of the file" when {

      "the xml is generated" in  {

        val sut = createSUT(pdfSubmission)

        val generatedXml = sut.toString()

        generatedXml(0) mustNot be('\n')
      }
    }

    "populate the correct header details" when {

      "the pdf submission xml is generated" in {

        val sut = createSUT(pdfSubmission)

        val doc = Jsoup.parse(sut.toString(), "", Parser.xmlParser)

        doc.select("ctutr > submissionReference").text() mustBe pdfSubmission.submissionReference
        doc.select("ctutr > dateCreated").text() mustBe pdfSubmission.xmlCreatedAt.toString("dd/MM/yyyy HH:mm:ss")
        doc.select("ctutr > companyName").text() mustBe submitValidViewModel.company.companyName
        doc.select("ctutr > companyReference").text() mustBe submitValidViewModel.company.companyReferenceNumber
      }
    }
  }

  val pdfSubmission: CTUTRMetadata = CTUTRMetadata(
    appConfig
  )

  private val submitValidViewModel: SubmissionViewModel = SubmissionViewModel(
    company = CompanyDetails(
      companyName = "Big company",
      companyReferenceNumber = "AB123123"
    ),
    timeStamp = "Tuesday 31 October 2017 at 15:18:12"
  )

  private def createSUT(pdfSubmission: CTUTRMetadata): Xml = templates.xml.robotXml(pdfSubmission, submitValidViewModel)
}
