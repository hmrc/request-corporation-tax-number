/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import com.google.inject.Singleton
import model.{Submission, FileUploadCallback}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubmissionController @Inject()(
                                   val submissionService: SubmissionService
                                   ) extends BaseController {

  def submit() : Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      request.body.validate[Submission].fold(
        errors => {
          Logger.warn(s"[SubmissionController][submit] Bad Request] $errors")
          Future.successful(BadRequest("invalid payload provided"))
        },
        e => {
            Logger.info(s"[SubmissionController][submit] processing submission")
            submissionService.submit(e) map {
              response =>
                Logger.info(s"[SubmissionController][submit] processed submission $response")
                Ok(Json.toJson(response))
            } recoverWith {
              case e : Exception =>
                Logger.error(s"[SubmissionController][submit][exception returned when processing submission] ${e.getMessage}")
                Future.successful(InternalServerError)
            }
          }
      )
  }

  def fileUploadCallback(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[FileUploadCallback] {
      fileUploadCallback =>
        Logger.info(s"[SubmissionController][fileUploadCallback] processing callback $fileUploadCallback")
        submissionService.fileUploadCallback(fileUploadCallback) map {
          _ =>
            Ok
        } recoverWith {
          case _ : Exception =>
            Future.successful(InternalServerError)
        }
    }
  }

}