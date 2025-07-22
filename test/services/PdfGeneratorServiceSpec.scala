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

package services

import helper.TestFixture
import model.{CompanyDetails, Submission}
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import org.apache.fop.apps.FopFactory
import play.api.Environment

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.Source
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.Loader
import templates.html.CTUTRScheme

class PdfGeneratorServiceSpec extends TestFixture {

  val env: Environment       = injector.instanceOf[Environment]
  val fopFactory: FopFactory = injector.instanceOf[FopFactory]

  private val pdfService: PdfGeneratorService = new PdfGeneratorService(fopFactory, env)

  "PdfGeneratorService" should {

    def extractTextFromBytes(pdfBytes: Array[Byte]): String = {
      val document: PDDocument = Loader.loadPDF(pdfBytes)
      val pdfStripper: PDFTextStripper = new PDFTextStripper()
      pdfStripper.getText(document)
    }

    "pdfService render " must {

      "generate the expected pdf" in {

        val submission: Submission = Submission(
          companyDetails = CompanyDetails(
            companyName = "company",
            companyReferenceNumber = "00000200"
          )
        )

        val time = LocalDateTime.parse("Friday 04 October 2024 12:17:18", DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy HH:mm:ss"))

        val metadata: CTUTRMetadata = CTUTRMetadata(
          appConfig = appConfig,
          createdAt = time
        )

        val submissionViewModel: SubmissionViewModel = SubmissionViewModel(submission, metadata)

        val response = pdfService.render(
          CTUTRScheme(submissionViewModel),
          Source.fromResource("CTUTRScheme.xml").mkString
        )

        val result: Array[Byte] = Await.result(response, Duration.Inf)

        val staticPdf = getClass
          .getResourceAsStream("/CTUTR_example_04102024.pdf")
          .readAllBytes()

        extractTextFromBytes(result) mustBe extractTextFromBytes(staticPdf)
      }
    }
  }
}
