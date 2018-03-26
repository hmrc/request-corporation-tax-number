package connectors

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.kenshoo.play.metrics.Metrics
import config.{MicroserviceAppConfig, SpecBase}
import model.domain.MimeContentType
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class FileUploadConnectorSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterEachTestData {

  override protected def beforeEach(testData: TestData): Unit = {
    reset(mockHttp)
    reset(mockClient)
    reset(metrics)
  }

  implicit val hc = HeaderCarrier()

  "createEnvelope" must {
    "return an envelope id" in {
      val sut = createSut

      Await.result(sut.createEnvelope, 5 seconds) mustBe "0b215e97-11d4-4006-91db-c067e74fc653"
    }

    "call the file upload service create envelope endpoint" in {
      val sut = createSut

      Await.result(sut.createEnvelope, 5.seconds)
      val envelopeBody = Json.parse(
        s"""
           |{
           |"callbackUrl": "${sut.callbackUrl}"
           |}
     """.stripMargin)

      verify(sut.httpClient, times(1)).POST(any(), Matchers.eq(envelopeBody), any())(any(), any(), any(), any())
    }
    "throw a runtime exception" when {
      "the success response does not contain a location header" in {
        val sut = createSut

        when(sut.httpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(201)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.createEnvelope, 5 seconds)

        ex.getMessage mustBe "File upload envelope creation failed"
      }
      "the call to the file upload service create envelope endpoint fails" in {
        val sut = createSut

        when(sut.httpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("call failed")))

        val ex = the[RuntimeException] thrownBy Await.result(sut.createEnvelope, 5 seconds)

        ex.getMessage mustBe "File upload envelope creation failed"

      }
      "the call to the file upload service returns a failure response" in {
        val sut = createSut

        when(sut.httpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(400)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.createEnvelope, 5 seconds)

        ex.getMessage mustBe "File upload envelope creation failed"

      }
    }
  }

  "uploadFile" must {

    "return Success" when {
      "File upload service successfully upload the file" in {
        val sut = createSut
        val mockWSResponse = createMockResponse(200, "")
        val mockWSRequest = mock[WSRequest]

        when(mockWSRequest.post(anyObject[Source[MultipartFormData.Part[Source[ByteString, _]], _]]()))
          .thenReturn(Future.successful(mockWSResponse))
        when(sut.wsClient.url(any())).thenReturn(mockWSRequest)
        when(mockWSRequest.withHeaders(any())).thenReturn(mockWSRequest)

        val result = Await.result(sut.uploadFile(new Array[Byte](1), fileName, contentType, envelopeId, fileId), 5 seconds)

        result.status mustBe 200
        verify(sut.wsClient, times(1)).url(Matchers.eq(s"file-upload-frontend/file-upload/upload/envelopes/$envelopeId/files/$fileId"))
      }
    }
    "throw runtime exception" when {
      "file upload service return status other than 200" in {
        val sut = createSut
        val mockWSResponse = createMockResponse(400, "")
        val mockWSRequest = mock[WSRequest]

        when(mockWSRequest.post(anyObject[Source[MultipartFormData.Part[Source[ByteString, _]], _]]()))
          .thenReturn(Future.successful(mockWSResponse))
        when(sut.wsClient.url(any())).thenReturn(mockWSRequest)
        when(mockWSRequest.withHeaders(any())).thenReturn(mockWSRequest)

        the[RuntimeException] thrownBy Await.result(sut.uploadFile(new Array[Byte](1), fileName, contentType, envelopeId, fileId), 5 seconds)
      }

      "any error occurred" in {
        val sut = createSut
        val mockWSRequest = mock[WSRequest]

        when(mockWSRequest.post(anyObject[Source[MultipartFormData.Part[Source[ByteString, _]], _]]()))
          .thenReturn(Future.failed(new RuntimeException("Error")))
        when(sut.wsClient.url(any())).thenReturn(mockWSRequest)
        when(mockWSRequest.withHeaders(any())).thenReturn(mockWSRequest)

        the[RuntimeException] thrownBy Await.result(sut.uploadFile(new Array[Byte](1), fileName, contentType, envelopeId, fileId), 5 seconds)
      }
    }

  }

  "closeEnvelope" must {
    "return an envelope id" in {
      val sut = createSut

      Await.result(sut.closeEnvelope(envelopeId), 5 seconds) mustBe envelopeId
    }

    "call the file upload service routing request endpoint" in {
      val sut = createSut

      Await.result(sut.closeEnvelope(envelopeId), 5.seconds)

      verify(sut.httpClient, times(1)).POST(Matchers.eq("file-upload/file-routing/requests"), any(), any())(any(), any(), any(), any())
    }
    "throw a runtime exception" when {
      "the success response does not contain a location header" in {
        val sut = createSut

        when(sut.httpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(201)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.closeEnvelope(envelopeId), 5 seconds)

        ex.getMessage mustBe "File upload envelope routing request failed"
      }
      "the call to the file upload service routing request endpoint fails" in {
        val sut = createSut

        when(sut.httpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("call failed")))

        val ex = the[RuntimeException] thrownBy Await.result(sut.closeEnvelope(envelopeId), 5 seconds)

        ex.getMessage mustBe "File upload envelope routing request failed"

      }
      "the call to the file upload service returns a failure response" in {
        val sut = createSut

        when(sut.httpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(400)))

        val ex = the[RuntimeException] thrownBy Await.result(sut.closeEnvelope(envelopeId), 5 seconds)

        ex.getMessage mustBe "File upload envelope routing request failed"

      }
    }
  }

  private def createMockResponse(status: Int, body: String): WSResponse = {
    val wsResponseMock = mock[WSResponse]
    when(wsResponseMock.status).thenReturn(status)
    when(wsResponseMock.body).thenReturn(body)
    wsResponseMock
  }

  def createSut = new SUT

  private val envelopeId: String = "0b215e97-11d4-4006-91db-c067e74fc653"
  private val fileId = "fileId"
  private val fileName = "fileName.pdf"
  private val contentType = MimeContentType.ApplicationPdf

  val mockHttp = mock[HttpClient]
  val mockClient = mock[WSClient]
  val metrics = mock[Metrics]

  class SUT extends FileUploadConnector(
    appConfig,
    mockHttp,
    mockClient,
    metrics
  ) {

    when(mockHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(201, None,
        Map("Location" -> Seq(s"localhost:8898/file-upload/envelopes/$envelopeId")))))

    when(mockHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(HttpResponse(201, None,
        Map("Location" -> Seq(s"/file-routing/requests/$envelopeId")))))

    override val fileUploadUrl: String = "file-upload"

    override val fileUploadFrontEndUrl: String = "file-upload-frontend"

    override val callbackUrl: String = "http://callback"
    
  }
}