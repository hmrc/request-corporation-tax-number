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

import model.dms.{NotificationRequest, SubmissionItemStatus}
import play.api.Logging
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.internalauth.client._

import javax.inject.{Inject, Singleton}

@Singleton
class DmsSubmissionCallbackController @Inject() (
    override val controllerComponents: ControllerComponents,
    auth: BackendAuthComponents
) extends BackendBaseController
    with Logging {

  private val predicate = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType("request-corporation-tax-number"),
      resourceLocation = ResourceLocation("dms/callback")
    ),
    action = IAAction("WRITE")
  )

  private val authorised = auth.authorizedAction(predicate)

  /** Callback function for DMS submission service.
    *
    *  This function is exposed via the '/dms-submission/callback' endpoint and is called by the dms-submission service
    *  to provide status updates on file submissions.
    *  This dms-submission service always expects an Ok response from this function.
    */
  def callback: Action[NotificationRequest] =
    authorised(parse.json[NotificationRequest]) { request =>
      val notification: NotificationRequest = request.body

      logger.info(
        s"[DmsSubmissionCallbackController][callback] DMS notification received for ${notification.id}"
      )

      if (notification.status == SubmissionItemStatus.Failed) {
        val failedReason =
          notification.failureReason.getOrElse("Error details not provided")
        logger.error(
          s"[DmsSubmissionCallbackController][callback] DMS notification received for ${notification.id} failed with error: $failedReason"
        )
      } else {
        logger.info(
          s"[DmsSubmissionCallbackController][callback] DMS notification received for ${notification.id} with status ${notification.status}"
        )
      }
      Ok
    }
}
