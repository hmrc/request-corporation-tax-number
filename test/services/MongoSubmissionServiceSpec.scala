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
import model.templates.CTUTRMetadata
import model.{CompanyDetails, Submission}
import org.bson.types.ObjectId
import org.bson.{BsonObjectId, BsonValue}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.Helpers._

import java.time.{Clock, Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.Future

class MongoSubmissionServiceSpec extends TestFixture {

  val mongoSubmissionService = new MongoSubmissionService(mockSubmissionMongoRepository, appConfig)

  val successfulInsertId = "6863ef672674b7459d411159"

  val successfulInsertOneResult: InsertOneResult = new InsertOneResult() {
    override def wasAcknowledged(): Boolean = true
    override def getInsertedId: BsonValue = new BsonObjectId(new ObjectId(successfulInsertId))
  }

  val unsuccessfulInsertOneResult: InsertOneResult = new InsertOneResult() {
    override def wasAcknowledged(): Boolean = true
    override def getInsertedId: BsonValue = null // if _id is not available this will return null
  }

  val notAcknowledgedInsertOneResult: InsertOneResult = new InsertOneResult() {
    override def wasAcknowledged(): Boolean = false
    override def getInsertedId: BsonValue = new BsonObjectId(new ObjectId(successfulInsertId))
  }

  val submission: Submission = Submission(
    companyDetails = CompanyDetails(
      companyName = "Big Company",
      companyReferenceNumber = "AB123123"
    )
  )

  val fixedClock: Clock = Clock.fixed(Instant.parse("2025-06-10T10:10:00Z"), ZoneOffset.UTC)

  val metadata: CTUTRMetadata = CTUTRMetadata(
    appConfig = appConfig,
    customerId = "testCustomer",
    clock = fixedClock
  )

  "MongoSubmissionService submit method" must {

    "return Ok with an objectId" when {

      "a valid submission is parsed to storeSubmission" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(Future.successful(successfulInsertOneResult))
        val result: String = await(mongoSubmissionService.storeSubmission(submission, metadata))
        assert(ObjectId.isValid(result))
        result mustBe successfulInsertId
      }

    }

    "return an internal server error" when {

      "the storesubmission method returns a DuplicateKeyException" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(
          Future.failed(new DuplicateKeyException(BsonDocument(), new ServerAddress(), WriteConcernResult.acknowledged(1, true, BsonString("Got a duplicate key!"))))
        )
        mongoSubmissionService.storeSubmission(submission, metadata).failed.futureValue shouldBe a [MongoException]
      }

      "the storeSubmission method returns a MongoException" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(
          Future.failed(new MongoException("Error writing to Mongo!!"))
        )
        mongoSubmissionService.storeSubmission(submission, metadata).failed.futureValue shouldBe a [MongoException]
      }

      "extracting the objectId returns null" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(
          Future.successful(unsuccessfulInsertOneResult)
        )
        mongoSubmissionService.storeSubmission(submission, metadata).failed.futureValue shouldBe a [MongoException]
      }

      "the storesubmission result was not acknowledged returns a MongoException" in {
        when(mockSubmissionMongoRepository.storeSubmission(any())).thenReturn(
          Future.successful(notAcknowledgedInsertOneResult)
        )
        mongoSubmissionService.storeSubmission(submission, metadata).failed.futureValue shouldBe a [MongoException]
      }
    }
  }

}
