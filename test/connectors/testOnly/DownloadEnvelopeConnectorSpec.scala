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

package connectors.testOnly

import helper.TestFixture
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.client.HttpClientV2

class DownloadEnvelopeConnectorSpec extends TestFixture {

  val mockHttpClientV2: HttpClientV2 = mock[HttpClientV2]
  val downloadEnvelopeRequest: DownloadEnvelopeRequest = mock[DownloadEnvelopeRequest]
  val downloadEnvelopeConnector: DownloadEnvelopeConnector = new DownloadEnvelopeConnector(appConfig, downloadEnvelopeRequest)

  "DownloadEnvelopeController" must {

    "return Ok and Download the envelope when the envelop exists" in {

      when(downloadEnvelopeRequest.executeGetRequest(any())(any(), any())).thenReturn(Future.successful(
        HttpResponse.apply(
          status = OK,
          bodyAsSource = Source.single(ByteString("test data")),
          headers = Map.empty
        ))
      )

      val result: Either[String, Source[ByteString, _]] = Await.result(downloadEnvelopeConnector.downloadEnvelopeRequest("TestEnvelope")(ec, hc), Duration.Inf)

      result mustBe a[Right[ByteString, _]]
      val actualByteString: ByteString = Await.result(result.toOption.get.runFold(ByteString.empty)(_ ++ _), Duration.Inf)
      actualByteString.utf8String mustBe "test data"
    }

    "return BadRequest with an error that indicates the envelop does not exists" in {

      when(downloadEnvelopeRequest.executeGetRequest(any())(any(), any())).thenReturn(Future.successful(
        HttpResponse.apply(
          status = BAD_REQUEST,
          body = "file does not exist",
          headers = Map.empty
        ))
      )
      val result: Either[String, Source[ByteString, _]] = Await.result(downloadEnvelopeConnector.downloadEnvelopeRequest("TestEnvelope")(ec, hc), Duration.Inf)

      result.isLeft mustBe true
      assert(result.swap.exists(_.contains("file does not exist")))
    }
  }
}
