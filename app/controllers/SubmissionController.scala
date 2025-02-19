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

import audit.{AuditService, CTUTRSubmission}
import com.google.inject.Singleton
import config.MicroserviceAppConfig

import javax.inject.Inject
import model.Submission
import model.templates.CTUTRMetadata
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CorrelationIdHelper

@Singleton
class SubmissionController @Inject()( val submissionService: SubmissionService,
                                      auditService: AuditService,
                                      cc: ControllerComponents,
                                      appConfig : MicroserviceAppConfig
                                    ) extends BackendController(cc)
  with Logging
  with CorrelationIdHelper {

    implicit val ec: ExecutionContext = cc.executionContext

    def submit(): Action[Submission] = Action.async(parse.json[Submission]) {
      implicit request =>

        implicit val hc: HeaderCarrier = getOrCreateCorrelationID(request)

        auditService.sendEvent(
          CTUTRSubmission(
            request.body.companyDetails.companyReferenceNumber,
            request.body.companyDetails.companyName
          )
        )

        logger.info(s"[SubmissionController][submit] processing submission")
        val ctutrMetadata: CTUTRMetadata = CTUTRMetadata(appConfig, request.body.companyDetails.companyReferenceNumber)

        submissionService.submit(ctutrMetadata, request.body).map{
          response =>
            logger.info(s"[SubmissionController][submit] processed submission $response")
            Ok(Json.toJson(response))
        }.recoverWith {
          case e: Exception =>
            logger.error(s"[SubmissionController][submit] Exception returned when processing submission: ${e.getMessage}")
            Future.successful(InternalServerError)
        }
    }
  }
