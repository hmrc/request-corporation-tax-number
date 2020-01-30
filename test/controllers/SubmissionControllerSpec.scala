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

package controllers

import audit.AuditService
import model.CallbackRequest
import model.domain.SubmissionResponse
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, _}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import services.SubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.test.UnitSpec
import util.MaterializerSupport

import scala.concurrent.Future

class SubmissionControllerSpec extends UnitSpec
  with OneAppPerSuite
  with MaterializerSupport
  with MockitoSugar {

  val submissionResponse = SubmissionResponse("12345", "12345-SubmissionCTUTR-20171023-iform.pdf")

  "SubmissionController" must {

    "return Ok with a envelopeId status" when {

      "valid payload is submitted" in {
        val sut = createSUT

        when(sut.submissionService.submit(any())(any())).thenReturn(Future.successful(submissionResponse))

        val result = Helpers.call(sut.submit(), fakeRequestValidDataset)
        status(result) shouldBe Status.OK
        contentAsJson(result).as[SubmissionResponse].id shouldBe "12345"
        contentAsJson(result).as[SubmissionResponse].filename shouldBe "12345-SubmissionCTUTR-20171023-iform.pdf"
      }

    }

    "return a non success http response" when {

      "submit fails to parse invalid payload" in {
        val sut = createSUT

        val result = Helpers.call(sut.submit(), fakeRequestBadRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "the submission service returns an error" in {
        val sut = createSUT

        when(sut.submissionService.submit(any())(any())).thenReturn(Future.failed(new InternalServerException("failed to process submission")))

        val result = Helpers.call(sut.submit(), fakeRequestValidDataset)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "fileUploadCallback" must {

    "return a 200 response status" when {

      "when available callback response" in {
        val sut = createSUT
        val callback = Json.toJson(CallbackRequest("env123", "file-id-1", "AVAILABLE"))

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = callback)

        when(sut.submissionService.callback(any())(any())).thenReturn(Future.successful("env123"))

        val result = Helpers.call(sut.fileUploadCallback(),fakeRequest)

        status(result) shouldBe OK
        verify(sut.submissionService, times(1)).callback(eqTo("env123"))(any())
      }

      "when closed callback response" in {
        val sut = createSUT
        val callback = Json.toJson(CallbackRequest("env123", "file-id-1", "CLOSED"))

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = callback)

        val result = Helpers.call(sut.fileUploadCallback(),fakeRequest)

        status(result) shouldBe OK
        verify(sut.submissionService, times(0)).callback("env123")
      }
    }
  }

  val validDataset = Json.parse(
    """
      |{
      |   "companyDetails": {
      |     "companyName": "Big Company",
      |     "companyReferenceNumber": "AB123123"
      |   }
      |}
      |""".stripMargin)


  val invalidDataset = Json.parse(
    """
      |{
      |   "companyDetails": {
      |     "company": "Bad Company",
      |     "reference": "XX123123"
      |   }
      |}
      |""".stripMargin)

  val fakeRequestValidDataset = FakeRequest("POST", "/submit").withJsonBody(validDataset)

  val fakeRequestBadRequest = FakeRequest("POST", "/submit").withJsonBody(invalidDataset)

  val mockSubmissionService = mock[SubmissionService]

  val mockAuditService = mock[AuditService]

  private class SUT extends SubmissionController(mockSubmissionService, mockAuditService)

  private def createSUT = new SUT

  private implicit val hc = HeaderCarrier()
}
