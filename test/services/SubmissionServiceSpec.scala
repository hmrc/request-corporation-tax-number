/*
 * Copyright 2019 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SubmissionServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = uk.gov.hmrc.http.HeaderCarrier()

  val mockFileUploadService: FileUploadService = mock[FileUploadService]
  val mockPdfService: PdfService = mock[PdfService]
  val mockSubmissionService: SubmissionService = spy(new SubmissionService(mockFileUploadService, mockPdfService, appConfig))


  val pdfBytes: Array[Byte] = Files.readAllBytes(Paths.get("test/resources/sample.pdf"))

  val submission: Submission = Submission(
    CompanyDetails (
      companyName = "Big company",
      companyReferenceNumber = "AB123123"
    )
  )

  val today = new LocalDate()
  val formatToday: String = today.toString("yyyyMMdd")

  "submit" must {

    "return a RuntimeException" when {

      "envelopeSummary fails" in {
        when(mockPdfService.generatePdf(any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.failed(new RuntimeException))

        val results: Future[SubmissionResponse] = mockSubmissionService.submit(submission)

        whenReady(results.failed) {
          results =>
            results.getMessage mustBe "Submit Failed"
            results mustBe a[RuntimeException]
        }
      }

      "createEnvelope fails" in {
        when(mockPdfService.generatePdf(any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()).thenReturn(Future.failed(new RuntimeException))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "OPEN", Some(Seq(File("", ""))))))

        val results: Future[SubmissionResponse] = mockSubmissionService.submit(submission)

        whenReady(results.failed) {
          results =>
            results.getMessage mustBe "Submit Failed"
            results mustBe a[RuntimeException]
        }
      }

      "generatePdf fails" in {
        when(mockPdfService.generatePdf(any())).thenReturn(Future.failed(new RuntimeException))

        when(mockFileUploadService.createEnvelope()).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "OPEN", Some(Seq(File("", ""))))))

        val results: Future[SubmissionResponse] = mockSubmissionService.submit(submission)

        whenReady(results.failed) {
          results =>
            results.getMessage mustBe "Submit Failed"
            results mustBe a[RuntimeException]
        }
      }

      "envelopeSummary is not OPEN" in {
        when(mockPdfService.generatePdf(any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "CLOSED", Some(Seq(File("", ""))))))

        val results: Future[SubmissionResponse] = mockSubmissionService.submit(submission)

        whenReady(results.failed) {
          results =>
            results.getMessage mustBe "Submit Failed"
            results mustBe a[RuntimeException]
        }
      }
    }
  }

  "submit" must {

    "return an SubmissionResponse" when {

      "given valid inputs" in {
        when(mockPdfService.generatePdf(any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "OPEN", Some(Seq(File("", ""))))))

        whenReady(mockSubmissionService.submit(submission)) {
          result =>
            verify(mockFileUploadService, atLeastOnce()).uploadFile(any(), any(), Matchers.eq(s"1-SubmissionCTUTR-$formatToday-iform.pdf"), any())(any())
            verify(mockFileUploadService, atLeastOnce()).uploadFile(any(), any(), Matchers.eq(s"1-SubmissionCTUTR-$formatToday-metadata.xml"), any())(any())
            verify(mockFileUploadService, atLeastOnce()).uploadFile(any(), any(), Matchers.eq(s"1-SubmissionCTUTR-$formatToday-robotic.xml"), any())(any())

            result mustEqual SubmissionResponse("1", s"1-SubmissionCTUTR-$formatToday-iform.pdf")
        }
      }
    }
  }


  "callback" must {

    "close the envelope" when {

      "envelope is open and all files are present and have passed file upload" in {
        when(mockFileUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN", Some(Seq(File("pdf","AVAILABLE"), File("metadata","AVAILABLE"), File("robot","AVAILABLE"))))
        ))
        when(mockFileUploadService.closeEnvelope("123")).thenReturn(Future.successful("123"))

        Await.result(mockSubmissionService.callback("123"), 5.seconds) mustBe "123"

      }
    }

    "return an envelopeId due to file upload failure" when {

      "envelope not open" in {
        when(mockFileUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"CLOSED",Some(Seq(File("pdf","AVAILABLE"), File("metadata","AVAILABLE"), File("robot","AVAILABLE"))))
        ))

        Await.result(mockSubmissionService.callback("123"), 5.seconds) mustBe "123"

      }

      "incorrect number of files" in {
        when(mockFileUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","ERROR"))))
        ))

        Await.result(mockSubmissionService.callback("123"), 5.seconds) mustBe "123"

      }

      "all files are not flagged as AVAILABLE" in {

        when(mockFileUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","ERROR"), File("metadata","ERROR"), File("robot","ERROR"))))
        ))

        Await.result(mockSubmissionService.callback("123"), 5.seconds) mustBe "123"

      }
    }
  }
}
