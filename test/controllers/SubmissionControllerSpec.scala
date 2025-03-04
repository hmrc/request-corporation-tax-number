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

package controllers

import helper.TestFixture
import model.domain.SubmissionResponse
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

class SubmissionControllerSpec extends TestFixture {

  val validDataset: JsValue = Json.parse(
    """
      |{
      |   "companyDetails": {
      |     "companyName": "Big Company",
      |     "companyReferenceNumber": "AB123123"
      |   }
      |}
      |""".stripMargin)


  val invalidDataset: JsValue = Json.parse(
    """
      |{
      |   "companyDetails": {
      |     "company": "Bad Company",
      |     "reference": "XX123123"
      |   }
      |}
      |""".stripMargin)

  val fakeRequestValidDataset: FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/submit").withJsonBody(validDataset)

  val fakeRequestBadRequest: FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/submit").withJsonBody(invalidDataset)

  val successSubmissionResponse: SubmissionResponse = SubmissionResponse("12345", "12345-SubmissionCTUTR-20171023-iform.pdf")
  val submissionController = new SubmissionController(mockSubmissionService, mockAuditService, stubCC, appConfig)

  "SubmissionController" must {

    "return Ok http response" when {

      "submit is called with valid payload" in {
        when(submissionController.submissionService.submit(any(), any())(any())).thenReturn(Future.successful(successSubmissionResponse))

        val result = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe Status.OK
        contentAsJson(result).as[SubmissionResponse].id mustBe "12345"
        contentAsJson(result).as[SubmissionResponse].filename mustBe "12345-SubmissionCTUTR-20171023-iform.pdf"
      }

    }

    "return a BAD_REQUEST http response" when {

      "submit is called with payload that cannot be parsed" in {
        val result = Helpers.call(submissionController.submit(), fakeRequestBadRequest)
        status(result) mustBe BAD_REQUEST
      }
    }

    "return an InternalServerError http response" when {

      Seq(
        ("RuntimeException", new RuntimeException(s"Failed with status [400]")),
        ("InternalServerException", new InternalServerException("failed to process submission"))
      ).foreach { errorStringAndError: (String, Exception) =>
        s"the submission service returns an ${errorStringAndError._1}" in {
          when(submissionController.submissionService.submit(any(), any())(any())).thenReturn(Future.failed(errorStringAndError._2))

          val result = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "getOrCreateCorrelationID" must {

    "add HEADER_X_CORRELATION_ID to header when not in request" in {
      val hc: HeaderCarrier = submissionController.getOrCreateCorrelationID(fakeRequestValidDataset)
      assert(hc.extraHeaders.map(_._1).contains(submissionController.HEADER_X_CORRELATION_ID))
    }

    "return original header if it contains HEADER_X_CORRELATION_ID already" in {
      val hc: HeaderCarrier = submissionController.getOrCreateCorrelationID(
        fakeRequestValidDataset.withHeaders((submissionController.HEADER_X_CORRELATION_ID, "1234"))
      )
      assert(hc.headers(Seq(submissionController.HEADER_X_CORRELATION_ID)).contains((submissionController.HEADER_X_CORRELATION_ID, "1234")))
    }
  }
}
