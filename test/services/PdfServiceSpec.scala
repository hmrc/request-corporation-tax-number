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

import connectors.PdfConnector
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HttpException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class PdfServiceSpec extends PlaySpec with MockitoSugar {

  "PdfService" should {

    "return the pdf as bytes " when {

      "generatePdf is called successfully" in {

        val htmlAsString = "<html>test</html>"

        val sut = createSut

        when(sut.pdfConnector.generatePdf(any())).thenReturn(Future.successful(htmlAsString.getBytes))

        val response = sut.generatePdf(htmlAsString)

        val result = Await.result(response, 5 seconds)

        result mustBe htmlAsString.getBytes
      }
    }

    "propagate an HttpException" when {

      "generatePdf is called and the pdf connector generates an HttpException" in {

        val htmlAsString = "<html>test</html>"

        val sut = createSut

        when(sut.pdfConnector.generatePdf(any())).thenReturn(Future.failed(new HttpException("", 0)))

        val result = sut.generatePdf(htmlAsString)

        the[HttpException] thrownBy Await.result(result, 5 seconds)
      }
    }
  }

  private def createSut = new PdfServiceTest()

  val mockPdfConnector = mock[PdfConnector]

  private class PdfServiceTest() extends PdfService(mockPdfConnector)
}
