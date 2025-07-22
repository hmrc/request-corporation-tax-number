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

package controllers

import com.mongodb.MongoException
import helper.TestFixture
import model.CallbackRequest
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, argThat, eq => eqTo}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.http.InternalServerException
import play.api.mvc.Result

import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, Future}

class SubmissionControllerSpec extends TestFixture {

  "SubmissionController submit method" must {

    "return Ok with a envelopeId status" when {

      "valid payload is submitted and store-submission-enabled is enabled" in new SubmissionControllerTestSetup(storeSubmissionEnabled = true) {
        stubSuccessfulStoreSubmission("1234")
        val result: Future[Result] = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        val test: Result = Await.result(result, Inf)
        status(result) mustBe Status.OK
        contentAsJson(result).as[SubmissionResponse].id mustBe "12345"
        contentAsJson(result).as[SubmissionResponse].filename mustBe "12345-SubmissionCTUTR-20171023-iform.pdf"

        verify(mockMongoSubmissionService, times(1)).storeSubmission(
          eqTo(validSubmission),
          argThat { metadata: CTUTRMetadata =>
            metadata.customerId == expectedCTUTRMetadata.customerId &&
              metadata.createdAt == expectedCTUTRMetadata.createdAt
          }
        )
      }

      "valid payload is submitted and store-submission-enabled is disabled" in new SubmissionControllerTestSetup(storeSubmissionEnabled = false) {
        val result: Future[Result] = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        val test: Result = Await.result(result, Inf)
        status(result) mustBe Status.OK
        contentAsJson(result).as[SubmissionResponse].id mustBe "12345"
        contentAsJson(result).as[SubmissionResponse].filename mustBe "12345-SubmissionCTUTR-20171023-iform.pdf"
        verify(mockMongoSubmissionService, times(0)).storeSubmission(any(), any())
      }
    }

    "return a non success http response" when {

      "submit fails to parse invalid payload" in new SubmissionControllerTestSetup(storeSubmissionEnabled = true) {
        val result = Helpers.call(submissionController.submit(), fakeRequestBadRequest)
        status(result) mustBe BAD_REQUEST
      }

      "the submission service returns an error" in new SubmissionControllerTestSetup(storeSubmissionEnabled = true) {
        stubSuccessfulStoreSubmission("1234")
        when(mockSubmissionService.submit(any(), any())(any())).thenReturn(Future.failed(new InternalServerException("failed to process submission")))
        val result: Future[Result] = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "storing the submission in mongo db fails throwing a MongoException" in new SubmissionControllerTestSetup(storeSubmissionEnabled = true) {
        stubFailedStoreSubmission(new MongoException("There was an error!!"))
        val result: Future[Result] = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "storing the submission in mongo db fails throwing a NullPointerException" in new SubmissionControllerTestSetup(storeSubmissionEnabled = true) {
        stubFailedStoreSubmission(new NullPointerException("There was an error!!"))
        val result: Future[Result] = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "fileUploadCallback" must {

    "return a 200 response status" when {

      "when available callback response" in new SubmissionControllerTestSetup(storeSubmissionEnabled = true) {
        val callback: JsValue = Json.toJson(CallbackRequest("env123", "file-id-1", "AVAILABLE"))

        val fakeRequest: FakeRequest[JsValue] = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = callback)

        when(submissionController.submissionService.callback(eqTo("env123"))(any())).thenReturn(Future.successful("env123"))

        val result: Future[Result] = Helpers.call(submissionController.fileUploadCallback(),fakeRequest)

        status(result) mustBe OK
      }

      "when closed callback response" in new SubmissionControllerTestSetup(storeSubmissionEnabled = true) {
        val callback: JsValue = Json.toJson(CallbackRequest("env123", "file-id-1", "CLOSED"))

        val fakeRequest: FakeRequest[JsValue] = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = callback)

        val result: Future[Result] = Helpers.call(submissionController.fileUploadCallback(),fakeRequest)

        status(result) mustBe OK
        verify(submissionController.submissionService, times(0)).callback("env123")
      }
    }
  }
}
