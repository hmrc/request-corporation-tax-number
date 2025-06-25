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

package services

import com.mongodb.{DuplicateKeyException, MongoException, ServerAddress, WriteConcernResult}
import helper.TestFixture
import model.{CompanyDetails, Submission}
import org.bson.types.ObjectId
import org.bson.{BsonObjectId, BsonValue}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.InsertOneResult
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._

import scala.concurrent.Future

class MongoSubmissionServiceSpec extends TestFixture {

  val mongoSubmissionService = new MongoSubmissionService(mockSubmissionMongoRepository, appConfig)

  val successfulInsertOneResult: InsertOneResult = new InsertOneResult() {
    override def wasAcknowledged(): Boolean = true
    override def getInsertedId: BsonValue = new BsonObjectId
  }

  val unsuccessfulInsertOneResult: InsertOneResult = new InsertOneResult() {
    override def wasAcknowledged(): Boolean = false
    override def getInsertedId: BsonValue = null // if _id is not available this will return null
  }

  val submission: Submission = Submission(
    CompanyDetails(
      "Big Company",
      "AB123123"
    )
  )

  "MongoSubmissionService submit method" must {

    "return Ok with an objectId" when {

      "a valid submission is parsed to storeSubmission" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(Future.successful(successfulInsertOneResult))
        val result: Future[Result] = mongoSubmissionService.storeSubmission(submission)
        status(result) mustBe Status.CREATED
        assert(ObjectId.isValid(contentAsJson(result).as[String]))
      }

    }

    "return an internal server error" when {

      "the storesubmission method returns a DuplicateKeyException" in {

        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(Future.failed(
          new DuplicateKeyException(
            BsonDocument(),
            new ServerAddress(),
            mock[WriteConcernResult]
          )
        ))
        val result: Future[Result] = mongoSubmissionService.storeSubmission(submission)
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }

      "the storesubmission method returns a MongoException" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(Future.failed(new MongoException("Error writing to Mongo!!")))
        val result: Future[Result] = mongoSubmissionService.storeSubmission(submission)
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }

      "extracting the objectId returns null" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(Future.successful(unsuccessfulInsertOneResult))
        val result: Future[Result] = mongoSubmissionService.storeSubmission(submission)
        status(result) mustBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

}
