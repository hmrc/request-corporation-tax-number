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
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import repositories.SubmissionRepository
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class SubmissionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEachTestData {

  override protected def beforeEach(testData: TestData): Unit = {
    reset(mockFUploadService)
    reset(mockSubmissionRepository)
    reset(mockPdfService)
  }

  "submit" must {

    "return an envelopeId" when {

      "given valid inputs" in {

        val pdfBytes = Files.readAllBytes(Paths.get("test/resources/sample.pdf"))
        val submissionDetails = SubmissionDetails(pdfUploaded = false, metadataUploaded = false, robotXmlUploaded = false)

        val sut = createSut

        when(sut.pdfService.generatePdf(any()))
          .thenReturn(Future.successful(pdfBytes))

        when(sut.fileUploadService.createEnvelope()).thenReturn(Future.successful("1"))

        when(sut.fileUploadService.uploadFile(any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(200)))

        when(sut.submissionRepository.updateSubmissionDetails(any(), any())(any())).thenReturn(Future.successful(submissionDetails))

        val result = Await.result(sut.submit(submission), 5.seconds)

        result mustBe SubmissionResponse("1", "1-SubmissionCTUTR-20171023-iform.pdf")

        verify(sut.fileUploadService, times(1)).uploadFile(any(), any(), Matchers.contains(s"1-SubmissionCTUTR-20171023-iform.pdf"), any())(any())
      }
    }
  }

  "SubmissionService" should {

    "close the envelope" when {

      "files are available" in {
        val sut = createSut
        when(sut.fileUploadService.closeEnvelope(any())(any())).thenReturn(Future.successful("123"))
        when(sut.submissionRepository.submissionDetails(any())(any())).thenReturn(Future.successful(Some(SubmissionDetails(pdfUploaded = true, metadataUploaded = false, robotXmlUploaded = false))))
        when(sut.submissionRepository.removeSubmissionDetails(any())(any())).thenReturn(Future.successful(true))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","metadata","AVAILABLE",None)), 5.seconds)

        result mustBe Closed
        verify(sut.fileUploadService, times(1)).closeEnvelope(Matchers.eq("123"))(any())
        verify(sut.submissionRepository, times(1)).removeSubmissionDetails(Matchers.eq("123"))(any())
      }

    }

    "not close the envelope" when {

      "received multiple callback for PDF" in {
        val sut = createSut
        when(sut.submissionRepository.submissionDetails(any())(any())).thenReturn(Future.successful(Some(SubmissionDetails(pdfUploaded = true, metadataUploaded = false, robotXmlUploaded = false))))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","SubmissionCTUTRiform","AVAILABLE",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
      }

      "received multiple callback for Robot" in {
        val sut = createSut
        when(sut.submissionRepository.submissionDetails(any())(any())).thenReturn(Future.successful(Some(SubmissionDetails(pdfUploaded = false, metadataUploaded = false, robotXmlUploaded = true))))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","robot","AVAILABLE",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
      }


      "received multiple callback for Metadata" in {
        val sut = createSut
        when(sut.submissionRepository.submissionDetails(any())(any())).thenReturn(Future.successful(Some(SubmissionDetails(pdfUploaded = false, metadataUploaded = true, robotXmlUploaded = false))))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","metadata","AVAILABLE",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
      }

      "received status other than Available or Error" in {
        val sut = createSut

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","SubmissionCTUTRiform","INFECTED",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
      }
    }

    "update the CTUTR details" when {

      "first callback received with status available" in {
        val sut = createSut
        when(sut.submissionRepository.updateSubmissionDetails(any(), any())(any())).thenReturn(Future.successful(SubmissionDetails(pdfUploaded = true, metadataUploaded = false, robotXmlUploaded = false)))
        when(sut.submissionRepository.submissionDetails(any())(any())).thenReturn(Future.successful(Some(SubmissionDetails(pdfUploaded = false, metadataUploaded = false, robotXmlUploaded = false))))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","SubmissionCTUTRiform","AVAILABLE",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
        verify(sut.submissionRepository, times(1)).updateSubmissionDetails(Matchers.eq("123"), any())(any())
      }
    }
  }

  implicit val hc = uk.gov.hmrc.http.HeaderCarrier()

  def createSut = new SUT

  private val submission: Submission = Submission(
    CompanyDetails (
      companyName = "Big company",
      companyReferenceNumber = "AB123123"
    )
  )

  val mockFUploadService = mock[FileUploadService]
  val mockSubmissionRepository = mock[SubmissionRepository]
  val mockPdfService = mock[PdfService]

  class SUT extends SubmissionService(mockFUploadService, mockPdfService, mockSubmissionRepository, appConfig) {
    override protected def submissionFileName(envelopeId: String): String = s"$envelopeId-SubmissionCTUTR-20171023-iform.pdf"
  }

}
