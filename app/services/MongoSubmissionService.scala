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
import play.api.libs.json.Json
import play.api.mvc.Result
import repositories.SubmissionMongoRepository

import javax.inject.Inject
import play.api.mvc.Results.{Created, InternalServerError, Ok}

import scala.concurrent.{ExecutionContext, Future}
import com.mongodb.{DuplicateKeyException, MongoException}
import config.MicroserviceAppConfig
import model.templates.CTUTRMetadata

class MongoSubmissionService @Inject()(
                                       val submissionMongoRepository: SubmissionMongoRepository,
                                       appConfig : MicroserviceAppConfig
                                      )(implicit ec: ExecutionContext) extends Logging {

  def storeSubmission(submission: Submission): Future[Result] = {
    val metadata: CTUTRMetadata = CTUTRMetadata(appConfig, submission.companyDetails.companyReferenceNumber)
    val mongoSubmission: MongoSubmission = MongoSubmission(submission, metadata)

    logger.info(s"[MongoSubmissionService][storeSubmission] Initialising storing of submission...")

    for {
      insertResult: InsertOneResult <- submissionMongoRepository.storeSubmission(mongoSubmission)
    } yield {
      val mongoSubmissionId: String = insertResult.getInsertedId.asObjectId().getValue.toString
      logger.info(s"[MongoSubmissionService][storeSubmission] Successfully stored submission. SubmissionId: ${mongoSubmissionId}")
      Created(Json.toJson(mongoSubmissionId))
    }
  }.recoverWith {
    case e: MongoException =>
      logger.error(s"[MongoSubmissionService][storeSubmission] MongoException returned when saving submission to Mongo, Error: ${e.getMessage}")
      Future.successful(InternalServerError)
    case e: NullPointerException =>
      logger.error(s"[MongoSubmissionService][storeSubmission] NullPointerException returned when saving submission to Mongo, Error: ${e.getMessage}")
      Future.successful(InternalServerError)
  }
}
