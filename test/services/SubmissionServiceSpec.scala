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

package services

import java.nio.file.{Files, Paths}

import config.SpecBase
import model._
import model.domain.SubmissionResponse
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HttpResponse
import play.api.test.Helpers._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class SubmissionServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  implicit val hc = uk.gov.hmrc.http.HeaderCarrier()

  val mockFUploadService = mock[FileUploadService]
  val mockPdfService = mock[PdfService]

  object Service extends SubmissionService(mockFUploadService, mockPdfService, appConfig)

  "submit" must {

    "return an envelopeId" when {

      "given valid inputs" in {

        val pdfBytes = Files.readAllBytes(Paths.get("test/resources/sample.pdf"))
        val submission: Submission = Submission(
          CompanyDetails (
            companyName = "Big company",
            companyReferenceNumber = "AB123123"
          )
        )

        when(mockPdfService.generatePdf(any()))
          .thenReturn(Future.successful(pdfBytes))

        when(mockFUploadService.createEnvelope()).thenReturn(Future.successful("1"))

        when(mockFUploadService.uploadFile(any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(OK)))

        whenReady(Service.submit(submission)) {
          result =>
            verify(mockFUploadService, times(1)).uploadFile(any(), any(), Matchers.contains(s"1-SubmissionCTUTR-20171023-iform.pdf"), any())(any())
            verify(mockFUploadService, times(1)).uploadFile(any(), any(), Matchers.contains(s"1-SubmissionCTUTR-20171023-metadata.xml"), any())(any())
            verify(mockFUploadService, times(1)).uploadFile(any(), any(), Matchers.contains(s"1-SubmissionCTUTR-20171023-robot.xml"), any())(any())
            result mustEqual SubmissionResponse("1", "1-SubmissionCTUTR-20171023-iform.pdf")
        }
      }
    }
  }

  "callback" must {

    "close the envelope" when {

      "envelope is open and all files are present and have passed file upload" in {
        when(mockFUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN", Some(Seq(File("pdf","AVAILABLE"), File("metadata","AVAILABLE"), File("robot","AVAILABLE"))))
        ))
        when(mockFUploadService.closeEnvelope("123")).thenReturn(Future.successful("123"))

        Await.result(Service.callback("123"), 5.seconds) mustBe "123"

      }
    }

    "return an envelopeId due to file upload failure" when {

      "envelope not open" in {
        when(mockFUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"CLOSED",Some(Seq(File("pdf","AVAILABLE"), File("metadata","AVAILABLE"), File("robot","AVAILABLE"))))
        ))

        Await.result(Service.callback("123"), 5.seconds) mustBe "123"

      }

      "incorrect number of files" in {
        when(mockFUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","ERROR"))))
        ))

        Await.result(Service.callback("123"), 5.seconds) mustBe "123"

      }

      "all files are not flagged as AVAILABLE" in {

        when(mockFUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","ERROR"), File("metadata","ERROR"), File("robot","ERROR"))))
        ))

        Await.result(Service.callback("123"), 5.seconds) mustBe "123"

      }
    }
  }
}
