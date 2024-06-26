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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HttpException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class PdfServiceSpec extends TestFixture {

  val pdfService: PdfService = new PdfService(mockPdfConnector)

  "PdfService" should {

    "return the pdf as bytes " when {

      "generatePdf is called successfully" in {

        val htmlAsString = "<html>test</html>"

        when(pdfService.pdfConnector.generatePdf(any())(any())).thenReturn(Future.successful(htmlAsString.getBytes))

        val response = pdfService.generatePdf(htmlAsString)

        val result = Await.result(response, 5 seconds)

        result mustBe htmlAsString.getBytes
      }
    }

    "propagate an HttpException" when {

      "generatePdf is called and the pdf connector generates an HttpException" in {

        val htmlAsString = "<html>test</html>"

        when(pdfService.pdfConnector.generatePdf(any())(any())).thenReturn(Future.failed(new HttpException("", 0)))

        val result = pdfService.generatePdf(htmlAsString)

        the[HttpException] thrownBy Await.result(result, 5 seconds)
      }
    }
  }
}
