/*
 * Copyright 2025 HM Revenue & Customs
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
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, FORBIDDEN, UNAUTHORIZED}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.Future

class DmsConnectorSpec extends TestFixture {

  val requestBuilder: RequestBuilder = mock[RequestBuilder]
  val httpClient: HttpClientV2 = mock[HttpClientV2]
  val dmsConnector = new DmsConnector(httpClient)(appConfig)

  when(httpClient.post(any())(any())).thenReturn(requestBuilder)
  when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
  when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)

  val today: LocalDate = LocalDate.now()
  val formatToday: String = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
  val submissionReference: String = UUID.randomUUID().toString
  val pdfFileName = "test.pdf"

  "postFileData" must {

    "return a SubmissionResponse" when {

      "the request to dms-submission returns ACCEPTED" in {
        val ctutrMetadata = CTUTRMetadata(appConfig, "customerId")
        when(requestBuilder.execute[HttpResponse](any(), any())).thenReturn(
          Future.successful(
            HttpResponse(
              status = ACCEPTED,
              body = """{"id":"71378476-272e-48c4-ac8f-b18af8dbc8f4"}"""
            )
          )
        )
        val submissionResponse: Future[SubmissionResponse] = dmsConnector.postFileData(
          ctutrMetadata,
          ByteString("pdf"),
          pdfFileName,
          ByteString("pdf"),
          "testRobot.xml"
        )
        whenReady(submissionResponse) {
          submissionResponse: SubmissionResponse =>
            submissionResponse mustBe SubmissionResponse(ctutrMetadata.submissionReference, pdfFileName)
        }
      }
    }

    "return a RuntimeException" when {
      Seq(
        BAD_REQUEST,
        UNAUTHORIZED,
        FORBIDDEN
      ).foreach{ exception: Int =>
        s"the call to DMS Submissions fails returning ${exception}" in {
          when(requestBuilder.execute[HttpResponse](any(), any()))
            .thenReturn(
              Future.failed(new RuntimeException(s"Failed with status [$exception]"))
            )
          val submissionResponse: Future[SubmissionResponse] = dmsConnector.postFileData(
            CTUTRMetadata(appConfig, "customerId"),
            ByteString("pdf"),
            pdfFileName,
            ByteString("pdf"),
            "testRobot.xml"
          )
          whenReady(submissionResponse.failed) {
            result: Throwable =>
              result mustBe a[RuntimeException]
              result.getMessage mustBe s"Failed with status [${exception}]"
          }
        }
      }
    }
  }

}
