package services

import connectors.FileUploadConnector
import model.domain.MimeContentType
import org.mockito.Matchers.any
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class FileUploadServiceSpec extends PlaySpec with MockitoSugar {

  "FileUploadService" must {

    "able to create enveloper" in {
      val sut = createSUT
      when(sut.fileUploadConnector.createEnvelope(any())).thenReturn(Future.successful("123"))

      val envelopeId = Await.result(sut.createEnvelope(), 5.seconds)

      envelopeId mustBe "123"
    }


    "able to upload the file" in {
      val sut = createSUT
      when(sut.fileUploadConnector.uploadFile(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = Await.result(sut.uploadFile(new Array[Byte](1), "123", fileName, contentType), 5.seconds)

      result.status mustBe 200
      Mockito.verify(sut.fileUploadConnector, Mockito.times(1)).uploadFile(any(), Matchers.eq(s"$fileName"),
        Matchers.eq(contentType), any(), Matchers.eq(s"$fileId"))(any())
    }

    "able to close the envelope" in {
      val sut = createSUT
      when(sut.fileUploadConnector.closeEnvelope(any())(any())).thenReturn(Future.successful("123"))

      val result = Await.result(sut.closeEnvelope("123"), 5.seconds)

      result mustBe "123"
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val fileName = "EnrolSocialCareCompliance.pdf"
  private val fileId = "EnrolSocialCareCompliance"
  private val contentType = MimeContentType.ApplicationPdf

  def createSUT = new SUT

  val mockFileUploadConnector = mock[FileUploadConnector]

  class SUT extends FileUploadService(mockFileUploadConnector)

}