/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionController @Inject()( val submissionService: SubmissionService,
                                      auditService: AuditService,
                                      cc: ControllerComponents
                                    ) extends BackendController(cc) {

  implicit val ec: ExecutionContext = cc.executionContext

  def submit() : Action[Submission] = Action.async(parse.json[Submission]) {
    implicit request =>
      auditService.sendEvent(
        CTUTRSubmission(
          request.body.companyDetails.companyReferenceNumber,
          request.body.companyDetails.companyName
        )
      )
      Logger.info(s"[SubmissionController][submit] processing submission")
      submissionService.submit(request.body) map {
        response =>
          Logger.info(s"[SubmissionController][submit] processed submission $response")
          Ok(Json.toJson(response))
      } recoverWith {
        case e : Exception =>
          Logger.error(s"[SubmissionController][submit][exception returned when processing submission] ${e.getMessage}")
          Future.successful(InternalServerError)
      }
  }

  def fileUploadCallback(): Action[CallbackRequest] =
    Action.async(parse.json[CallbackRequest]) {
      implicit request =>
        Logger.info(s"[SubmissionController][fileUploadCallback] processing callback ${request.body}")
        if (request.body.status == "AVAILABLE") {
          submissionService.callback(request.body.envelopeId).map {
            _ =>
              Ok
          }
        } else {
          Logger.info(s"[SubmissionController][fileUploadCallback] callback for ${request.body.fileId} had status: ${request.body.status}")
          Future.successful(Ok)
        }
    }
}
