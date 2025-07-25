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
import model._
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import java.time._
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SubmissionServiceSpec extends TestFixture {

  val submissionService: SubmissionService = new SubmissionService(mockFileUploadService, mockPdfService, ec)

  val pdfBytes: Array[Byte] = getClass
    .getResourceAsStream("/CTUTR_example_04102024.pdf")
    .readAllBytes()

  val submission: Submission = Submission(
    CompanyDetails (
      companyName = "Big company",
      companyReferenceNumber = "AB123123"
    )
  )

  val metadata: CTUTRMetadata = CTUTRMetadata(appConfig)

  val today = LocalDate.now()
  val formatToday: String = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

  "submit" must {

    "return a RuntimeException" when {

      "envelopeSummary fails" in {
        when(mockPdfService.render(any(), any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()(any())).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.failed(new RuntimeException))

        val results: Future[SubmissionResponse] = submissionService.submit(submission, metadata)

        whenReady(results.failed) {
          results =>
            results.getMessage mustBe "Submit Failed"
            results mustBe a[RuntimeException]
        }
      }

      "createEnvelope fails" in {
        when(mockPdfService.render(any(), any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()(any())).thenReturn(Future.failed(new RuntimeException))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "OPEN", Some(Seq(File("", ""))))))

        val results: Future[SubmissionResponse] = submissionService.submit(submission, metadata)

        whenReady(results.failed) {
          results =>
            results.getMessage mustBe "Submit Failed"
            results mustBe a[RuntimeException]
        }
      }

      "generatePdf fails" in {
        when(mockPdfService.render(any(), any())).thenReturn(Future.failed(new RuntimeException))

        when(mockFileUploadService.createEnvelope()(any())).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "OPEN", Some(Seq(File("", ""))))))

        val results: Future[SubmissionResponse] = submissionService.submit(submission, metadata)

        whenReady(results.failed) {
          results =>
            results.getMessage mustBe "Submit Failed"
            results mustBe a[RuntimeException]
        }
      }

      "envelopeSummary is not OPEN" in {
        when(mockPdfService.render(any(), any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()(any())).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "CLOSED", Some(Seq(File("", ""))))))

        val results: Future[SubmissionResponse] = submissionService.submit(submission, metadata)

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
        when(mockPdfService.render(any(), any())).thenReturn(Future.successful(pdfBytes))

        when(mockFileUploadService.createEnvelope()(any())).thenReturn(Future.successful("1"))

        when(mockFileUploadService.envelopeSummary(any())(any())).thenReturn(Future.successful(Envelope("", Some(""), "OPEN", Some(Seq(File("", ""))))))

        whenReady(submissionService.submit(submission, metadata)) {
          result =>
            verify(mockFileUploadService, atLeastOnce()).uploadFile(any(), any(), eqTo(s"1-SubmissionCTUTR-$formatToday-iform.pdf"), any())(any())
            verify(mockFileUploadService, atLeastOnce()).uploadFile(any(), any(), eqTo(s"1-SubmissionCTUTR-$formatToday-metadata.xml"), any())(any())
            verify(mockFileUploadService, atLeastOnce()).uploadFile(any(), any(), eqTo(s"1-SubmissionCTUTR-$formatToday-robotic.xml"), any())(any())

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

        Await.result(submissionService.callback("123"), 5.seconds) mustBe "123"

      }
    }

    "return an envelopeId due to file upload failure" when {

      "envelope not open" in {
        when(mockFileUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"CLOSED",Some(Seq(File("pdf","AVAILABLE"), File("metadata","AVAILABLE"), File("robot","AVAILABLE"))))
        ))

        Await.result(submissionService.callback("123"), 5.seconds) mustBe "123"

      }

      "incorrect number of files" in {
        when(mockFileUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","ERROR"))))
        ))

        Await.result(submissionService.callback("123"), 5.seconds) mustBe "123"

      }

      "all files are not flagged as AVAILABLE" in {

        when(mockFileUploadService.envelopeSummary("123")).thenReturn(Future.successful(
          Envelope("123",Some("callback"),"OPEN",Some(Seq(File("pdf","ERROR"), File("metadata","ERROR"), File("robot","ERROR"))))
        ))

        Await.result(submissionService.callback("123"), 5.seconds) mustBe "123"

      }
    }
  }

  "createMetadata and createRobotXml" must {

    "create and contain identical submissionReference values" when {

      "the metadata is created" in {

        val metadata = CTUTRMetadata(appConfig)
        val pdfSubmissionMetadata = submissionService.createMetadata(metadata)
        val robotXml = submissionService.createRobotXml(submission, metadata)

        val pdfMetadataDoc = Jsoup.parse(pdfSubmissionMetadata.mkString("Array(", ", ", ")"), "", Parser.xmlParser)
        val robotXmlDoc = Jsoup.parse(robotXml.mkString("Array(", ", ", ")"), "", Parser.xmlParser)

        pdfMetadataDoc.select("header > title").text() mustBe robotXmlDoc.select("ctutr > submissionReference").text()

      }
    }
  }

}
