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

package controllers

import model.FileUploadCallback
import model.domain.SubmissionResponse
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, _}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import services.{SubmissionService, Open}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.test.UnitSpec
import util.MaterializerSupport

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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

        when(sut.submissionService.submit(any())(any())).thenReturn(Future.failed(new InternalServerException("failed to process enrolment")))

        val result = Helpers.call(sut.submit(), fakeRequestValidDataset)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "company fileUploadCallback" must {

    "return a 200 response status" when {

      "envelope id is provided in callback" in {
        val sut = createSUT
        val json =
          """{
            |  "envelopeId": "0b215ey97-11d4-4006-91db-c067e74fc653",
            |  "fileId": "file-id-1",
            |  "status": "ERROR",
            |  "reason": "VirusDetected"
            |}""".stripMargin

        when(sut.submissionService.fileUploadCallback(any())(any())).thenReturn(Future.successful(Open))

        val jsValue = Json.parse(json)
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = jsValue)

        val result = Await.result(sut.fileUploadCallback()(fakeRequest), 5.seconds)

        result.header.status shouldBe 200
        verify(sut.submissionService, times(1)).fileUploadCallback(Matchers.eq(jsValue.as[FileUploadCallback]))(any())
      }

    }

    "return InternalServerError when a callback is for a submission that does not exist in mongo" ignore {
      val sut = createSUT
      val json =
        """{
          |  "envelopeId": "0b215ey97-11d4-4006-91db-c067e74fc653",
          |  "fileId": "file-id-1",
          |  "status": "ERROR",
          |  "reason": "VirusDetected"
          |}""".stripMargin

      when(sut.submissionService.submissionRepository.submissionDetails(any())(any())).thenReturn(Future.successful(None))

      val jsValue = Json.parse(json)
      val fakeRequest = FakeRequest(method = "POST", uri = "",
        headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = jsValue)

      val result = Await.result(sut.fileUploadCallback()(fakeRequest), 5.seconds)

      result.header.status shouldBe 500
      verify(sut.submissionService, times(1)).fileUploadCallback(Matchers.eq(jsValue.as[FileUploadCallback]))(any())
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

  private class SUT extends SubmissionController(mockSubmissionService)

  private def createSUT = new SUT

  private implicit val hc = HeaderCarrier()
}
