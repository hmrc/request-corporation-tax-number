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

package controllers.testOnly

import connectors.testOnly.DownloadEnvelopeConnector
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.CorrelationIdHelper

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DownloadEnvelopeController @Inject()(downloadEnvelopeConnector: DownloadEnvelopeConnector,
                                           cc: ControllerComponents
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging with CorrelationIdHelper {

  def downloadEnvelope(envelopeId: String): Action[AnyContent] =
    Action.async { implicit request =>
      downloadEnvelopeConnector.downloadEnvelopeRequest(envelopeId).map {
        case Right(source: Source[ByteString, _]) =>
          Ok.streamed(source, None)
            .withHeaders(
              CONTENT_TYPE        -> "application/zip",
              CONTENT_DISPOSITION -> s"""attachment; filename = "${envelopeId}.zip""""
            )
        case Left(error) => BadRequest(error)
      }
    }
}
