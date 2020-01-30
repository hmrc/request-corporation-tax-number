/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import akka.util.ByteString
import com.kenshoo.play.metrics.Metrics
import config.SpecBase
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito._
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.hmrc.http.HttpException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class PdfConnectorSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterEachTestData {

  override protected def beforeEach(testData: TestData): Unit = {
    reset(mockClient)
    reset(mockMetrics)
  }

  "PdfConnector" should {

    "return the basicUrl with serviceUrl prepended" when {
      "the service url is supplied" in {

        val sut = createSut("test-service-url")

        sut.basicUrl mustBe "test-service-url/pdf-generator-service/generate"
      }
    }

    "return the basicUrl without the serviceUrl prepended" when {
      "the service url is not supplied" in {

        val sut = createSut()

        sut.basicUrl mustBe "/pdf-generator-service/generate"
      }
    }

    "return the pdfServiceUrl as the service url" when {

      "the PDFConnector object is created" in {
        val connector = app.injector.instanceOf[PdfConnector]
        connector.serviceUrl mustBe connector.appConfig.pdfServiceUrl
      }
    }

    "return the pdf service payload in bytes " when {
      "generatePdf is called successfully" in {

        val htmlAsString = "<html>test</html>"

        val sut = createSut()

        val mockWSResponse = createMockResponse(200, htmlAsString)

        val mockWSRequest = mock[WSRequest]
        when(mockWSRequest.post(anyString())(any())).thenReturn(Future.successful(mockWSResponse))

        when(sut.wsClient.url(any())).thenReturn(mockWSRequest)

        val response = sut.generatePdf(htmlAsString)

        val result = Await.result(response, 5 seconds)

        result mustBe htmlAsString.getBytes

        verify(sut.wsClient, times(1)).url(sut.basicUrl)
        verify(mockWSRequest, times(1)).post(anyString())(any())
      }
    }

    "generate an HttpException" when {

      "generatePdf is called and the pdf service returns something other than 200" in {

        val htmlAsString = "<html>test</html>"

        val sut = createSut()

        val mockWSResponse = createMockResponse(400, "")

        val mockWSRequest = mock[WSRequest]
        when(mockWSRequest.post(anyString())(any())).thenReturn(Future.successful(mockWSResponse))

        when(sut.wsClient.url(any())).thenReturn(mockWSRequest)

        val result = sut.generatePdf(htmlAsString)

        the[HttpException] thrownBy Await.result(result, 5 seconds)

        verify(sut.wsClient, times(1)).url(sut.basicUrl)
        verify(mockWSRequest, times(1)).post(anyString())(any())
      }
    }
  }

  private def createMockResponse(status: Int, body: String): WSResponse = {

    val wsResponseMock = mock[WSResponse]

    when(wsResponseMock.status).thenReturn(status)
    when(wsResponseMock.body).thenReturn(body)
    when(wsResponseMock.bodyAsBytes).thenReturn(ByteString(body))

    wsResponseMock
  }

  val mockClient = mock[WSClient]
  val mockMetrics = mock[Metrics]

  private def createSut(testServiceUrl: String = "") = new PdfConnectorTest(testServiceUrl)

  private class PdfConnectorTest(testServiceUrl: String = "") extends PdfConnector(appConfig, mockClient, mockMetrics) {
    override def serviceUrl: String = testServiceUrl
  }
}
