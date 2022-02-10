/*
 * Copyright 2022 HM Revenue & Customs
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

package audit

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.libs.json.{JsString, Json, Writes}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {

  def sendEvent[T <: AuditEvent](event: T)(implicit
                                           rh: RequestHeader,
                                           write: Writes[T],
                                           ec: ExecutionContext): Future[AuditResult]

}

@Singleton
class AuditServiceImpl @Inject()(
                                 auditConnector: AuditConnector
                                ) extends AuditService with Logging {

  private implicit def toHc(request: RequestHeader): AuditHeaderCarrier =
    auditHeaderCarrier(HeaderCarrierConverter.fromRequestAndSession(request, request.session))

  def sendEvent[T <: AuditEvent](event: T)(implicit
                                           rh: RequestHeader,
                                           write: Writes[T],
                                           ec: ExecutionContext): Future[AuditResult] = {

    val eventJson = Json.obj(
      "data" -> event
    )

    val details = rh.toAuditTags().foldLeft(eventJson) {
      case (m, (k, v)) =>
        m + (k -> JsString(v))
    }

    logger.debug(s"[AuditService][sendEvent] sending ${event.auditType}")

    val result: Future[AuditResult] = auditConnector.sendExtendedEvent(ExtendedDataEvent(
      auditSource = "request-corporation-tax-number",
      auditType = event.auditType,
      tags = rh.toAuditTags(
        transactionName = event.auditType,
        path = rh.path
      ),
      detail = details
    ))

    result.foreach { _ =>
        logger.debug(s"[AuditService][sendEvent] successfully sent ${event.auditType}")
    }

    result.failed.foreach { e =>
        logger.error(s"[AuditService][sendEvent] failed to send event ${event.auditType}", e)
    }

    result
  }

}
