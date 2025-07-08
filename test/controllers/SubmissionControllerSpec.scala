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
import org.mongodb.scala.result.InsertOneResult
import play.api.mvc.Results.Created
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.LocalDateTime
import scala.concurrent.Future

class SubmissionControllerSpec extends TestFixture {

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

  val submissionResponse: SubmissionResponse = SubmissionResponse("12345", "12345-SubmissionCTUTR-20171023-iform.pdf")
  val submissionController = new SubmissionController(mockMongoSubmissionService, mockSubmissionService, mockSubmissionMongoRepository, mockAuditService, appConfig, stubCC)

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
        when(mockMongoSubmissionService.storeSubmission(any())).thenReturn(Future.successful("1234"))
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
        when(mockMongoSubmissionService.storeSubmission(any())).thenReturn(Future.successful("1234"))
        when(submissionController.auditSubmission(any(), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        when(submissionController.submissionService.submit(any())(any())).thenReturn(Future.failed(new InternalServerException("failed to process submission")))

        val result = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "storing the submission in mongo db fails throwing a MongoException" in {
        when(mockMongoSubmissionService.storeSubmission(any())).thenReturn(Future.failed(new MongoException("There was an error!!")))
        val result = Helpers.call(submissionController.submit(), fakeRequestValidDataset)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "storing the submission in mongo db fails throwing a NullPointerException" in {
        when(mockMongoSubmissionService.storeSubmission(any())).thenReturn(Future.failed(new NullPointerException("There was an error!!")))
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
