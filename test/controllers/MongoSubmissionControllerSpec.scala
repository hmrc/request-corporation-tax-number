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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.mvc.Results.Created
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future

class MongoSubmissionControllerSpec extends TestFixture {

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

  val fakeRequestValidDataset: FakeRequest[AnyContentAsJson] =
    FakeRequest("POST", "/store-submission").withJsonBody(validDataset)

  val fakeRequestBadRequest: FakeRequest[AnyContentAsJson] =
    FakeRequest("POST", "/store-submission").withJsonBody(invalidDataset)

  val mongoSubmissionController = new MongoSubmissionController(mockMongoSubmissionService, stubCC)

  "MongoSubmissionControllerSpec storeSubmission method" must {

    "return Created with the ObjectId" when {

      "valid payload is submitted" in {
        when(mockMongoSubmissionService.storeSubmission(any())).thenReturn(Future.successful(Created(Json.toJson("1234"))))
        val result = Helpers.call(mongoSubmissionController.storeSubmission(), fakeRequestValidDataset)
        status(result) mustBe Status.CREATED
        contentAsJson(result).as[String] mustBe "1234"
      }
    }

    "return a non success http response" when {

      "submit fails to parse invalid payload" in {
        val result = Helpers.call(mongoSubmissionController.storeSubmission(), fakeRequestBadRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
  }
  }
