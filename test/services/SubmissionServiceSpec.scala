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

package services

import helper.TestFixture
import model._
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status._

import java.time._
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.Future

class SubmissionServiceSpec extends TestFixture {

  val submissionService: SubmissionService = new SubmissionService(mockDmsConnector, mockPdfService)

  val pdfBytes: Array[Byte] = getClass
    .getResourceAsStream("/CTUTR_example_04102024.pdf")
    .readAllBytes()

  val submission: Submission = Submission(
    CompanyDetails (
      companyName = "Big company",
      companyReferenceNumber = "AB123123"
    )
  )

  val today = LocalDate.now()
  val formatToday: String = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
  val defaultSubmissionReference: String = UUID.randomUUID().toString
  val ctutrMetadata: CTUTRMetadata = CTUTRMetadata(appConfig)

  "submit" must {

    "return a RuntimeException" when {

      "pdf service render fails" in {
        when(mockPdfService.render(any(), any())).thenReturn(Future.failed(new RuntimeException("error")))
        val results: Future[SubmissionResponse] = submissionService.submit(ctutrMetadata, submission)
        whenReady(results.failed) {
          results: Throwable =>
            results.getMessage mustBe "[SubmissionService][createPdf] Error creating PDF: error"
            results mustBe a[RuntimeException]
        }
      }
    }

    "return a SubmissionResponse" when {

      "given valid inputs and the call to DMS Submissions is ACCEPTED" in {
        when(mockPdfService.render(any(), any())).thenReturn(Future.successful(pdfBytes))
        when(mockDmsConnector.postFileData(any(), any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(
            Future.successful(
              SubmissionResponse(defaultSubmissionReference, s"${defaultSubmissionReference}-SubmissionCTUTR-$formatToday-iform.pdf")
            )
          )
        whenReady(submissionService.submit(ctutrMetadata, submission)) {
          result =>
            result mustEqual SubmissionResponse(defaultSubmissionReference, s"${defaultSubmissionReference}-SubmissionCTUTR-$formatToday-iform.pdf")
        }
      }
    }

    "return a RuntimeException" when {
      Seq(
        BAD_REQUEST,
        UNAUTHORIZED,
        FORBIDDEN
      ).foreach{ exception: Int =>
        s"the call to DMS Submissions fails returning ${exception}" in {
          when(mockPdfService.render(any(), any())).thenReturn(Future.successful(pdfBytes))
          when(mockDmsConnector.postFileData(any(), any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(
              Future.failed(new RuntimeException(s"Failed with status [$exception]"))
            )
          whenReady(submissionService.submit(ctutrMetadata, submission).failed) {
            result: Throwable =>
              result mustBe a[RuntimeException]
              result.getMessage mustBe s"Failed with status [${exception}]"
          }
        }
      }
    }
  }
}
