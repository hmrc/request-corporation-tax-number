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

import helper.TestFixture
import model.{CallbackRequest, MongoSubmission}
import model.domain.SubmissionResponse
import org.bson.{BsonObjectId, BsonType, BsonValue}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.http.InternalServerException
import org.bson.types.ObjectId
import org.mongodb.scala.result.InsertOneResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.LocalDateTime
import java.util.Date
import scala.concurrent.Future

class SubmissionControllerSpec extends TestFixture {

  val objectId = "684ffb301ec3e3567ca327fb"

  val validDataset: JsValue = Json.parse(s"\"${objectId}\"")

  val invalidDataset: JsValue = Json.parse(
    """
      |{
      |   "NOT_AN_ID": "1234"
      |}
      |""".stripMargin)

  val fakeRequestValidDataset = FakeRequest("POST", "/submit").withJsonBody(validDataset)

  val fakeRequestBadRequest = FakeRequest("POST", "/submit").withJsonBody(invalidDataset)

  val submissionResponse: SubmissionResponse = SubmissionResponse("12345", "12345-SubmissionCTUTR-20171023-iform.pdf")
  val submissionController = new SubmissionController(mockSubmissionService, mockSubmissionMongoRepository, mockAuditService, stubCC)

  val successfulInsertOneResult: InsertOneResult = new InsertOneResult() {
    override def wasAcknowledged(): Boolean = true
    override def getInsertedId: BsonValue = new BsonObjectId
  }

  val mongoSubmission: MongoSubmission = MongoSubmission(
    companyName = "Big Company",
    companyReferenceNumber = "AB123123",
    time = LocalDateTime.now(),
    submissionReference = "submission-123"
  )

  val unsuccessfulInsertOneResult: InsertOneResult = new InsertOneResult() {
    override def wasAcknowledged(): Boolean = false
    override def getInsertedId: BsonValue = new BsonObjectId
  }

  "SubmissionController submit method" must {

    "return Ok with a envelopeId status" when {

      "valid payload is submitted" in {
        when(mockSubmissionMongoRepository.getOneSubmission(eqTo(objectId))).thenReturn(Future.successful(Seq(mongoSubmission)))
        when(submissionController.submissionService.submit(any())(any())).thenReturn(Future.successful(submissionResponse))
        when(submissionController.auditSubmission(any(), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

        val result = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe Status.OK
        contentAsJson(result).as[SubmissionResponse].id mustBe "12345"
        contentAsJson(result).as[SubmissionResponse].filename mustBe "12345-SubmissionCTUTR-20171023-iform.pdf"
      }

    }

    "return a non success http response" when {

      "submit fails to parse invalid payload" in {
        val result = Helpers.call(submissionController.submit(), fakeRequestBadRequest)
        status(result) mustBe BAD_REQUEST
      }

      "the submission service returns an error" in {
        when(mockSubmissionMongoRepository.getOneSubmission(eqTo(objectId))).thenReturn(Future.successful(Seq(mongoSubmission)))
        when(submissionController.auditSubmission(any(), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        when(submissionController.submissionService.submit(any())(any())).thenReturn(Future.failed(new InternalServerException("failed to process submission")))

        val result = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "fileUploadCallback" must {

    "return a 200 response status" when {

      "when available callback response" in {
        val callback = Json.toJson(CallbackRequest("env123", "file-id-1", "AVAILABLE"))

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = callback)

        when(submissionController.submissionService.callback(eqTo("env123"))(any())).thenReturn(Future.successful("env123"))

        val result = Helpers.call(submissionController.fileUploadCallback(),fakeRequest)

        status(result) mustBe OK
      }

      "when closed callback response" in {
        val callback = Json.toJson(CallbackRequest("env123", "file-id-1", "CLOSED"))

        val fakeRequest = FakeRequest(method = "POST", uri = "",
          headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = callback)

        val result = Helpers.call(submissionController.fileUploadCallback(),fakeRequest)

        status(result) mustBe OK
        verify(submissionController.submissionService, times(0)).callback("env123")
      }
    }
  }
}
