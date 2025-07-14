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

package services

import model.{MongoSubmission, Submission}
import org.mongodb.scala.result.InsertOneResult
import play.api.Logging
import repositories.SubmissionMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import com.mongodb.MongoException
import config.MicroserviceAppConfig
import model.templates.CTUTRMetadata
import org.mongodb.scala.DuplicateKeyException

class MongoSubmissionService @Inject()(
                                       val submissionMongoRepository: SubmissionMongoRepository,
                                       appConfig : MicroserviceAppConfig
                                      )(implicit ec: ExecutionContext) extends Logging {

  def storeSubmission(submission: Submission, metadata: CTUTRMetadata): Future[String] = {
    logger.info(s"[MongoSubmissionService][storeSubmission] Initialising storing of submission...")
    val mongoSubmission: MongoSubmission = MongoSubmission(submission, metadata)
    (for {
      insertResult: InsertOneResult <- submissionMongoRepository.storeSubmission(mongoSubmission)
    } yield {
      if (insertResult.wasAcknowledged()) {
        val mongoSubmissionId: String = insertResult.getInsertedId.asObjectId().getValue.toString
        logger.info(s"[MongoSubmissionService][storeSubmission] Successfully stored submission. SubmissionId: ${mongoSubmissionId}")
        mongoSubmissionId
      }
      else {
        throw new MongoException("Insert was unsuccessful, insertOneResult was not acknowledged.")
      }
    }).recoverWith {
      case e: NullPointerException =>
        logger.error(s"[MongoSubmissionService][storeSubmission] NullPointerException returned when saving submission to Mongo, Error: ${e.getMessage}")
        throw new MongoException("Insert was unsuccessful, received null pointer.")
      case e: DuplicateKeyException =>
        logger.error(s"[MongoSubmissionService][storeSubmission] DuplicateKeyException returned when saving submission to Mongo, Error: ${e.getMessage}")
        throw new MongoException("Insert was unsuccessful, received duplicate key exception.")
    }
  }
}
