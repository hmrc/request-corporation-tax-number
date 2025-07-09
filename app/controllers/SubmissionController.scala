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
import com.mongodb.MongoException
import config.MicroserviceAppConfig

import javax.inject.Inject
import model.{CallbackRequest, MongoSubmission, Submission}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Request}
import repositories.SubmissionMongoRepository
import services.{MongoSubmissionService, SubmissionService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CorrelationIdHelper
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import uk.gov.hmrc.play.audit.http.connector.AuditResult

@Singleton
class SubmissionController @Inject()(val mongoSubmissionService: MongoSubmissionService,
                                     val submissionService: SubmissionService,
                                     val submissionMongoRepository: SubmissionMongoRepository,
                                     auditService: AuditService,
                                     appConfig : MicroserviceAppConfig,
                                     cc: ControllerComponents
                                    ) extends BackendController(cc) with Logging with CorrelationIdHelper {

  implicit val ec: ExecutionContext = cc.executionContext

  def submit() : Action[Submission] = Action.async(parse.json[Submission]) {
    implicit request: Request[Submission] =>
      implicit val hc: HeaderCarrier = getOrCreateCorrelationID(request)

      logger.info(s"[SubmissionController][submit] processing submission")

      val metadata: CTUTRMetadata = CTUTRMetadata(appConfig, request.body.companyDetails.companyReferenceNumber)
      val mongoSubmission: MongoSubmission = MongoSubmission(request.body, metadata)
      (for {
        _ <- mongoSubmissionService.storeSubmission(mongoSubmission)
        _ <- auditSubmission(mongoSubmission)
        submitResult: SubmissionResponse <- submissionService.submit(mongoSubmission)
      } yield {
        logger.info(s"[SubmissionController][submit] processed submission $submitResult")
        Ok(Json.toJson(submitResult))
      }).recoverWith {
        case e: MongoException =>
          logger.error(s"[MongoSubmissionService][storeSubmission] MongoException returned when saving submission to Mongo, Error: ${e.getMessage}")
          Future.successful(InternalServerError)
        case e: Exception =>
          logger.error(s"[SubmissionController][submit][exception returned when processing submission] ${e.getMessage}")
          Future.successful(InternalServerError)
      }
  }

  def auditSubmission(mongoSubmission: MongoSubmission)
                     (implicit hc: HeaderCarrier, request: Request[Submission]): Future[AuditResult] =
    auditService.sendEvent(
      CTUTRSubmission(
        mongoSubmission.companyDetails.companyReferenceNumber,
        mongoSubmission.companyDetails.companyName
      )
    )

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
