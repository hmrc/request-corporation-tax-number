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

import audit.{AuditService, CTUTRSubmission}
import com.google.inject.Singleton

import javax.inject.Inject
import model.{CallbackRequest, Submission}
import org.bson.types.ObjectId
import org.mongodb.scala.result.InsertOneResult
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Request}
import repositories.SubmissionMongoRepository
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CorrelationIdHelper
import com.mongodb.MongoException

@Singleton
class SubmissionController @Inject()( val submissionService: SubmissionService,
                                      val submissionMongoRepository: SubmissionMongoRepository,
                                      auditService: AuditService,
                                      cc: ControllerComponents
                                    ) extends BackendController(cc) with Logging with CorrelationIdHelper {

  implicit val ec: ExecutionContext = cc.executionContext

  def submit() : Action[Submission] = Action.async(parse.json[Submission]) {
    implicit request: Request[Submission] =>
      implicit val hc: HeaderCarrier = getOrCreateCorrelationID(request)
      auditService.sendEvent(
        CTUTRSubmission(
          request.body.companyDetails.companyReferenceNumber,
          request.body.companyDetails.companyName
        )
      )
      logger.info(s"[SubmissionController][submit] processing submission")

      // NOTE: Store submission in submissions collection
      for {
        insertResult: InsertOneResult <- submissionMongoRepository.storeSubmission(request.body)
      } yield (
        if (insertResult.wasAcknowledged()){
          val submissionId: ObjectId = insertResult.getInsertedId.asObjectId().getValue
          logger.info(s"[SubmissionController][submit] Successfully stored submission. mongo submissionId: ${submissionId}")
        } else {
          logger.info(s"[SubmissionController][submit] Failed to store submission.")
          throw new MongoException("Failed to store submission")
        }
      )

      submissionService.submit(request.body) map {
        response =>
          logger.info(s"[SubmissionController][submit] processed submission $response")
          Ok(Json.toJson(response))
      } recoverWith {
        case mongoWriteException: MongoException =>
          logger.error(s"[SubmissionController][submit][exception returned when processing submission] ${mongoWriteException.getMessage}")
          Future.successful(InternalServerError)
        case e : Exception =>
          logger.error(s"[SubmissionController][submit][exception returned when processing submission] ${e.getMessage}")
          Future.successful(InternalServerError)
      }
  }

  def fileUploadCallback(): Action[CallbackRequest] =
    Action.async(parse.json[CallbackRequest]) {
      implicit request =>
        logger.info(s"[SubmissionController][fileUploadCallback] processing callback ${request.body}")
        if (request.body.status == "AVAILABLE") {
          submissionService.callback(request.body.envelopeId).map {
            _ =>
              Ok
          }
        } else {
          logger.info(s"[SubmissionController][fileUploadCallback] callback for ${request.body.fileId} had status: ${request.body.status}")
          Future.successful(Ok)
        }
    }
}
