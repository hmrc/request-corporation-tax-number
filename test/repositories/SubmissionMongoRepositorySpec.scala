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

package repositories

import helper.TestFixture
import model.{CompanyDetails, FlatSubmission, Submission}
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.mongo.test.MongoSupport

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

class SubmissionMongoRepositorySpec extends TestFixture with MongoSupport with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }

  val submissionMongoRepository = new SubmissionMongoRepository(appConfig, mongoComponent)

  val companyDetails: CompanyDetails = CompanyDetails(
    "initialSub",
    "12345"
  )
  val submission: Submission = new Submission(companyDetails)

  "SubmissionMongoRepository" must {

    "read and write a valid Submission" in {
      val expectedFlatSubmission = FlatSubmission.fromSubmission(submission)
      val storedSubmissions: Future[Seq[FlatSubmission]] = for {
        insertOneResult: InsertOneResult <- submissionMongoRepository.storeSubmission(submission)
        storedSubs <- submissionMongoRepository.getOneSubmission(insertOneResult.getInsertedId.asObjectId().getValue)
      } yield (storedSubs)
      Await.result(storedSubmissions, 30.seconds) must contain(expectedFlatSubmission)
      Await.result(mongoDatabase.getCollection(appConfig.submissionCollectionName).countDocuments().toFuture(), 30.seconds) mustBe(1)
    }

    "read and write multiple valid Submission" in {
      val secondSubmission = new Submission(companyDetails.copy(companyName = "secondSub"))
      val thirdSubmission = new Submission(companyDetails.copy(companyName = "thirdSub"))
      val forthSubmission = new Submission(companyDetails.copy(companyName = "forthSub"))

      val expectedFlatSubmissions: Seq[FlatSubmission] = Seq(
        FlatSubmission.fromSubmission(submission),
        FlatSubmission.fromSubmission(secondSubmission),
        FlatSubmission.fromSubmission(thirdSubmission),
        FlatSubmission.fromSubmission(forthSubmission)
      )

      val storedSubmission: Future[Seq[FlatSubmission]] = for {
        firstInsertOneResult: InsertOneResult <- submissionMongoRepository.storeSubmission(submission)
        secondInsertOneResult: InsertOneResult <- submissionMongoRepository.storeSubmission(secondSubmission)
        thirdInsertOneResult: InsertOneResult <- submissionMongoRepository.storeSubmission(thirdSubmission)
        forthInsertOneResult: InsertOneResult <- submissionMongoRepository.storeSubmission(forthSubmission)
        firstRetrievedSub: Seq[FlatSubmission] <- submissionMongoRepository.getOneSubmission(firstInsertOneResult.getInsertedId.asObjectId().getValue)
        secondRetrievedSub: Seq[FlatSubmission] <- submissionMongoRepository.getOneSubmission(secondInsertOneResult.getInsertedId.asObjectId().getValue)
        thirdRetrievedSub: Seq[FlatSubmission] <- submissionMongoRepository.getOneSubmission(thirdInsertOneResult.getInsertedId.asObjectId().getValue)
        forthRetrievedSub: Seq[FlatSubmission] <- submissionMongoRepository.getOneSubmission(forthInsertOneResult.getInsertedId.asObjectId().getValue)
      } yield (Seq(firstRetrievedSub, secondRetrievedSub, thirdRetrievedSub, forthRetrievedSub).flatten)

      Await.result(storedSubmission, 30.seconds) must contain theSameElementsAs(expectedFlatSubmissions)
      Await.result(mongoDatabase.getCollection(appConfig.submissionCollectionName).countDocuments().toFuture(), 30.seconds) mustBe(4)
    }
  }
}
