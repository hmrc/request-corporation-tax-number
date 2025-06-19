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
import model.{CallbackRequest, CompanyDetails, MongoSubmission, Submission}
import org.bson.types.ObjectId
import org.mongodb.scala.result.InsertOneResult
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import repositories.SubmissionMongoRepository
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CorrelationIdHelper
import com.mongodb.MongoException
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import uk.gov.hmrc.play.audit.http.connector.AuditResult

@Singleton
class SubmissionController @Inject()( val submissionService: SubmissionService,
                                      val submissionMongoRepository: SubmissionMongoRepository,
                                      auditService: AuditService,
                                      cc: ControllerComponents
                                    ) extends BackendController(cc) with Logging with CorrelationIdHelper {

  implicit val ec: ExecutionContext = cc.executionContext

  def submit() : Action[String] = Action.async(parse.json[String]) {
    implicit request: Request[String] =>
      implicit val hc: HeaderCarrier = getOrCreateCorrelationID(request)

      logger.info(s"[SubmissionController][submit] processing submission")

      (for {
        storedSubmission: MongoSubmission <- submissionMongoRepository.getOneSubmission(request.body).map(_.head)
        _ <- auditSubmission(storedSubmission.companyName, storedSubmission.companyReferenceNumber)
        submitResult: SubmissionResponse <- submissionService.submit(storedSubmission)
      } yield {
        logger.info(s"[SubmissionController][submit] processed submission $submitResult")
        Ok(Json.toJson(submitResult))
      }).recoverWith {
        case e : Exception =>
          logger.error(s"[SubmissionController][submit][exception returned when processing submission] ${e.getMessage}")
          Future.successful(InternalServerError)
      }
  }

  def auditSubmission(companyReferenceNumber: String, companyName: String)
                     (implicit hc: HeaderCarrier, request: Request[String]): Future[AuditResult] =
    auditService.sendEvent(
      CTUTRSubmission(
        companyReferenceNumber,
        companyName
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
