package services

import java.nio.file.{Files, Paths}

import config.{MicroserviceAppConfig, SpecBase}
import model.domain.EnrolmentResponse
import model.{Employer, Enrolment, FileUploadCallback, IFormDetails}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import repositories.EnrolmentRepository
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class EnrolmentServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEachTestData {

  override protected def beforeEach(testData: TestData): Unit = {
    reset(mockFUploadService)
    reset(mockEnrolmentRepository)
    reset(mockPdfService)
  }

  "enrol" must {

    "return an envelopeId" when {

      "given valid inputs" in {

        val pdfBytes = Files.readAllBytes(Paths.get("test/resources/sample.pdf"))
        val iFormDetails = IFormDetails(pdfUploaded = false, metadataUploaded = false)

        val sut = createSut

        when(sut.pdfService.generatePdf(any()))
          .thenReturn(Future.successful(pdfBytes))

        when(sut.fileUploadService.createEnvelope()).thenReturn(Future.successful("1"))

        when(sut.fileUploadService.uploadFile(any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(200)))

        when(sut.enrolmentRepository.updateIFormDetails(any(), any())(any())).thenReturn(Future.successful(iFormDetails))

        val result = Await.result(sut.enrol(enrolment), 5.seconds)

        result mustBe EnrolmentResponse("1", "1-EnrolSocialCareCompliance-20171023-iform.pdf")

        verify(sut.fileUploadService, times(1)).uploadFile(any(), any(), Matchers.contains(s"1-EnrolSocialCareCompliance-20171023-iform.pdf"), any())(any())
      }
    }
  }

  "EnrolmentService" should {

    "close the envelope" when {

      "files are available" in {
        val sut = createSut
        when(sut.fileUploadService.closeEnvelope(any())(any())).thenReturn(Future.successful("123"))
        when(sut.enrolmentRepository.iFormDetails(any())(any())).thenReturn(Future.successful(Some(IFormDetails(pdfUploaded = true, metadataUploaded = false))))
        when(sut.enrolmentRepository.removeIFormDetails(any())(any())).thenReturn(Future.successful(true))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","metadata","AVAILABLE",None)), 5.seconds)

        result mustBe Closed
        verify(sut.fileUploadService, times(1)).closeEnvelope(Matchers.eq("123"))(any())
        verify(sut.enrolmentRepository, times(1)).removeIFormDetails(Matchers.eq("123"))(any())
      }

    }

    "not close the envelope" when {

      "received multiple callback for PDF" in {
        val sut = createSut
        when(sut.enrolmentRepository.iFormDetails(any())(any())).thenReturn(Future.successful(Some(IFormDetails(pdfUploaded = true, metadataUploaded = false))))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","EnrolSocialCareComplianceiform","AVAILABLE",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
      }

      "received multiple callback for Metadata" in {
        val sut = createSut
        when(sut.enrolmentRepository.iFormDetails(any())(any())).thenReturn(Future.successful(Some(IFormDetails(pdfUploaded = false, metadataUploaded = true))))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","metadata","AVAILABLE",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
      }

      "received status other than Available or Error" in {
        val sut = createSut

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","EnrolSocialCareComplianceiform","INFECTED",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
      }
    }

    "update the iform details" when {

      "first callback received with status available" in {
        val sut = createSut
        when(sut.enrolmentRepository.updateIFormDetails(any(), any())(any())).thenReturn(Future.successful(IFormDetails(pdfUploaded = true, metadataUploaded = false)))
        when(sut.enrolmentRepository.iFormDetails(any())(any())).thenReturn(Future.successful(Some(IFormDetails(pdfUploaded = false, metadataUploaded = false))))

        val result = Await.result(sut.fileUploadCallback(FileUploadCallback("123","EnrolSocialCareComplianceiform","AVAILABLE",None)), 5.seconds)

        result mustBe Open
        verify(sut.fileUploadService, never()).closeEnvelope(Matchers.eq("123"))(any())
        verify(sut.enrolmentRepository, times(1)).updateIFormDetails(Matchers.eq("123"), any())(any())
      }
    }
  }

  implicit val hc = uk.gov.hmrc.http.HeaderCarrier()

  def createSut = new SUT

  private val enrolment: Enrolment = Enrolment(
    capacityRegistering = "Individual",
    agent = None,
    employer = Employer(
      name = "Company",
      ukAddress = None,
      internationalAddress = None,
      telephoneNumber = None,
      emailAddress = None,
      taxpayerReference = None,
      payeReference = None
    )
  )

  val mockFUploadService = mock[FileUploadService]
  val mockEnrolmentRepository = mock[EnrolmentRepository]
  val mockPdfService = mock[PdfService]

  class SUT extends EnrolmentService(mockFUploadService, mockPdfService, mockEnrolmentRepository, appConfig) {
    override protected def enrolmentFileName(envelopeId: String): String = s"$envelopeId-EnrolSocialCareCompliance-20171023-iform.pdf"
  }

}
