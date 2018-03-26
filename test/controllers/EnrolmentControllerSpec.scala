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

import model.domain.EnrolmentResponse
import model.{FileUploadCallback, IFormDetails}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, _}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import services.{EnrolmentService, Open}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.test.UnitSpec
import util.MaterializerSupport

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class EnrolmentControllerSpec extends UnitSpec
  with OneAppPerSuite
  with MaterializerSupport
  with MockitoSugar {

  val enrolmentResponse = EnrolmentResponse("12345", "12345-EnrolSocialCareCompliance-20171023-iform.pdf")

  "EnrolmentController" must {

    "return Ok with a envelopeId status" when {

      "minimum valid payload is submitted" in {
        val sut = createSUT

        when(sut.enrolmentService.enrol(any())(any())).thenReturn(Future.successful(enrolmentResponse))

        val result = Helpers.call(sut.enrol(), fakeRequestMinimumDataset)
        status(result) shouldBe Status.OK
        contentAsJson(result).as[EnrolmentResponse].id shouldBe "12345"
        contentAsJson(result).as[EnrolmentResponse].filename shouldBe "12345-EnrolSocialCareCompliance-20171023-iform.pdf"
      }

      "maximum valid payload is submitted" in {
        val sut = createSUT

        when(sut.enrolmentService.enrol(any())(any())).thenReturn(Future.successful(enrolmentResponse))

        val result = Helpers.call(sut.enrol(), fakeRequestMaximumUKDataset)
        status(result) shouldBe Status.OK
        contentAsJson(result).as[EnrolmentResponse].id shouldBe "12345"
        contentAsJson(result).as[EnrolmentResponse].filename shouldBe "12345-EnrolSocialCareCompliance-20171023-iform.pdf"
      }
    }

    "return a non success http response" when {

      "enrol fails to parse invalid payload" in {
        val sut = createSUT

        val result = Helpers.call(sut.enrol(), fakeRequestBadRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "the enrolment service returns an error" in {
        val sut = createSUT

        when(sut.enrolmentService.enrol(any())(any())).thenReturn(Future.failed(new InternalServerException("failed to process enrolment")))

        val result = Helpers.call(sut.enrol(), fakeRequestMinimumDataset)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "employment fileUploadCallback" must {

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

        when(sut.enrolmentService.fileUploadCallback(any())(any())).thenReturn(Future.successful(Open))

        val jsValue = Json.parse(json)
        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = jsValue)

        val result = Await.result(sut.fileUploadCallback()(fakeRequest), 5.seconds)

        result.header.status shouldBe 200
        verify(sut.enrolmentService, times(1)).fileUploadCallback(Matchers.eq(jsValue.as[FileUploadCallback]))(any())
      }

    }

    "return InternalServerError when a callback is for an iForm that does not exist in mongo" ignore {
      val sut = createSUT
      val json =
        """{
          |  "envelopeId": "0b215ey97-11d4-4006-91db-c067e74fc653",
          |  "fileId": "file-id-1",
          |  "status": "ERROR",
          |  "reason": "VirusDetected"
          |}""".stripMargin

      when(sut.enrolmentService.enrolmentRepository.iFormDetails(any())(any())).thenReturn(Future.successful(None))

      val jsValue = Json.parse(json)
      val fakeRequest = FakeRequest(method = "POST", uri = "",
        headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = jsValue)

      val result = Await.result(sut.fileUploadCallback()(fakeRequest), 5.seconds)

      result.header.status shouldBe 500
      verify(sut.enrolmentService, times(1)).fileUploadCallback(Matchers.eq(jsValue.as[FileUploadCallback]))(any())
    }

  }

  val minimumDataset = Json.parse(
    """
      |{
      |   "capacityRegistering" : "Agent",
      |   "employer": {
      |     "name": "Test Company",
      |     "telephoneNumber": "0191 2221111"
      |   }
      |}
      |""".stripMargin)

  val maximumUKDataset = Json.parse(
    """
      |{
      |   "capacityRegistering" : "Agent",
      |   "employer": {
      |     "name" : "Test Company",
      |     "telephoneNumber": "0191 2221111",
      |     "ukAddress": {
      |       "addressLine1": "test",
      |       "addressLine2": "test",
      |       "addressLine3": "test",
      |       "addressLine4": "test",
      |       "addressLine5": "test",
      |       "postcode": "NE991PB"
      |     },
      |     "emailAddress": "test@company.com",
      |     "taxpayerReference": "1010101010101",
      |     "payeReference": "jdhdiwehwndnsdjn"
      |   },
      |   "agent": {
      |     "name": "John Smith",
      |     "ukAddress": {
      |       "addressLine1": "test",
      |       "addressLine2": "test",
      |       "addressLine3": "test",
      |       "addressLine4": "test",
      |       "addressLine5": "test",
      |       "postcode": "NE991PB"
      |     },
      |     "telephoneNumber": "0191 3332222",
      |     "emailAddress": "test@test.com"
      |   }
      |}
      |""".stripMargin)

  val badDataset = Json.parse(
    """
      |{
      |   "capacityRegistering" : "Agent",
      |   "agent": {
      |     "name": "John Smith",
      |     "ukAddress": {
      |       "addressLine1": "test",
      |       "addressLine2": "test",
      |       "addressLine3": "test",
      |       "addressLine4": "test",
      |       "addressLine5": "test",
      |       "postcode": "NE991PB"
      |     },
      |     "telephoneNumber": "0191 3332222",
      |     "emailAddress": "test@test.com"
      |   }
      |}
      |""".stripMargin)

  val fakeRequestMinimumDataset = FakeRequest("POST", "/enrol").withJsonBody(minimumDataset)
  val fakeRequestMaximumUKDataset = FakeRequest("POST", "/enrol").withJsonBody(maximumUKDataset)

  val fakeRequestBadRequest = FakeRequest("POST", "/enrol").withJsonBody(badDataset)

  val mockEnrolmentService = mock[EnrolmentService]

  private class SUT extends EnrolmentController(mockEnrolmentService)

  private def createSUT = new SUT

  private implicit val hc = HeaderCarrier()
}
