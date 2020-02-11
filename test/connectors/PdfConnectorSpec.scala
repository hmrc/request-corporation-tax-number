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

import akka.http.scaladsl.model.HttpCharsets
import helper.TestFixture
import org.eclipse.jetty.util.Utf8LineParser
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterEachTestData, TestData}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.http.{HttpException, HttpResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class PdfConnectorSpec extends TestFixture
  with BeforeAndAfterEachTestData with IntegrationPatience {

  override protected def beforeEach(testData: TestData): Unit = {
    reset(mockHttpClient)
    reset(mockMetrics)
  }

  val pdfConnector: PdfConnector = new PdfConnector(appConfig, mockHttpClient, mockMetrics, ec)

  "PdfConnector" should {

    "return the pdf service payload in bytes " when {
      "generatePdf is called successfully" in {

        val htmlAsString = "<html>test</html>"

        val httpResponse = HttpResponse(200, None, responseString = Some(htmlAsString))

        when(mockHttpClient.doPost(anyString(), any(), any())(any(), any(), any()))
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

        val httpResponse = HttpResponse(400, None)

        when(mockHttpClient.doPost(anyString(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = pdfConnector.generatePdf(htmlAsString)

        the[HttpException] thrownBy Await.result(result, 5 seconds)
      }
    }
  }
}
