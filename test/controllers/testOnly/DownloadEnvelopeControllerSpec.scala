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

package controllers.testOnly

import connectors.testOnly.DownloadEnvelopeConnector
import helper.TestFixture
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future

class DownloadEnvelopeControllerSpec extends TestFixture {

  val downloadEnvelopeConnector: DownloadEnvelopeConnector = mock[DownloadEnvelopeConnector]
  val downloadEnvelopeController: DownloadEnvelopeController = new DownloadEnvelopeController(
    downloadEnvelopeConnector = downloadEnvelopeConnector,
    cc = stubCC
  )

  val fakeRequestDownloadEnvelope: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/test-only/download/envelopes/TestEnvelope")

  def mockDownloadEnvelopeRequest(response: Either[String, Source[ByteString, _]]): OngoingStubbing[Future[Either[String, Source[ByteString, _]]]] =
    when(downloadEnvelopeConnector.downloadEnvelopeRequest(any())(any(), any())).thenReturn(Future.successful(response))

  "DownloadEnvelopeController" must {

    "return Ok and Download the envelope when the envelop exists" in {

      mockDownloadEnvelopeRequest(Right(Source.single(ByteString("test data"))))

      val result: Future[Result] = Helpers.call(
          downloadEnvelopeController.downloadEnvelope("TestEnvelope"),
          fakeRequestDownloadEnvelope
        )

        status(result) mustBe Status.OK
        headers(result) mustBe Map(
          CONTENT_TYPE-> "application/zip",
          CONTENT_DISPOSITION -> s"""attachment; filename = "TestEnvelope.zip""""
        )
      }

    "return BadRequest with an error that indicates the envelop does not exists" in {

      mockDownloadEnvelopeRequest(Left("file does not exist"))

      val result: Future[Result] = Helpers.call(
        downloadEnvelopeController.downloadEnvelope("TestEnvelope"),
        fakeRequestDownloadEnvelope
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include("file does not exist")
    }
  }
}
