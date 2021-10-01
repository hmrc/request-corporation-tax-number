/*
 * Copyright 2021 HM Revenue & Customs
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

import helper.TestFixture
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import uk.gov.hmrc.http.HttpException
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import uk.gov.hmrc.http.HttpResponse

class PdfConnectorSpec extends TestFixture
  with BeforeAndAfterEachTestData with IntegrationPatience {

  override protected def beforeEach(testData: TestData): Unit = {
    reset(mockHttpClient)
    reset(mockMetrics)
  }

  val pdfConnector: PdfConnector = new PdfConnector(appConfig, mockHttpClient, ec)

  val gernerateUrl = s"http://localhost:9203/pdf-generator-service/generate"
  val body = Map("html" -> List("<html>test</html>"))

  "PdfConnector" should {

    "return the pdf service payload in bytes " when {
      "generatePdf is called successfully" in {

        val htmlAsString = "<html>test</html>"
        val httpResponse = createMockResponse(200, htmlAsString)

        when(mockHttpClient.POST[Map[String,Seq[String]], HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val response = pdfConnector.generatePdf(htmlAsString)

        whenReady(response) { res =>
          res mustBe htmlAsString.getBytes
        }
      }
    }

    "generate an HttpException" when {

      "generatePdf is called and the pdf service returns something other than 200" in {

        val htmlAsString = "<html>test</html>"
        val httpResponse = createMockResponse(400, "")

        when(mockHttpClient.POST[Map[String,Seq[String]], HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = pdfConnector.generatePdf(htmlAsString)

        the[HttpException] thrownBy Await.result(result, 5 seconds)
      }
    }
  }

  private def createMockResponse(status: Int, body: String): HttpResponse = {

    val httpResponseMock = mock[HttpResponse]

    when(httpResponseMock.status).thenReturn(status)
    when(httpResponseMock.body).thenReturn(body)

    httpResponseMock
  }
}
