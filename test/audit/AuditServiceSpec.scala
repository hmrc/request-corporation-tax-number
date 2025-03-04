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

package audit

import helper.TestFixture
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.Future

class AuditServiceSpec extends TestFixture {

  private implicit val request: FakeRequest[AnyContentAsJson] = FakeRequest()
    .withHeaders("a" -> "B")
    .withJsonBody(Json.parse(
      """
        |{
        |   "companyDetails": {
        |     "companyName": "Big Company",
        |     "companyReferenceNumber": "AB123123"
        |   }
        |}
        |""".stripMargin))

  val auditService = new AuditServiceImpl(mockAuditConnector)

  ".sendEvent" must {

    "send an audit event and return a success when it works" in {

      when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val eventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      whenReady(auditService.sendEvent(CTUTRSubmission("foo", "bar"))) {
        result =>

          verify(mockAuditConnector, times(1)).sendExtendedEvent(eventCaptor.capture())(any(), any())
          eventCaptor.getValue.auditSource mustBe "request-corporation-tax-number"
          eventCaptor.getValue.auditType mustBe "CTUTRSubmission"
          eventCaptor.getValue.detail mustBe Json.obj(
            "data" -> CTUTRSubmission(
              "foo", "bar"
            ),
            "path" -> "/",
            "X-Session-ID" -> "-",
            "X-Request-ID" -> "-",
            "clientIP" -> "-",
            "clientPort" -> "-",
            "Akamai-Reputation" -> "-",
            "deviceID" -> "-"
          )
          eventCaptor.getValue.tags must contain(
            "transactionName" -> "CTUTRSubmission"
          )
          result mustEqual AuditResult.Success
      }
    }

    "return failure when the audit event fails" in {
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Failure("")))

      val result = auditService.sendEvent(CTUTRSubmission("foo", "bar"))

      result.map {
        x => x mustBe AuditResult.Failure
      }

    }
  }
}
