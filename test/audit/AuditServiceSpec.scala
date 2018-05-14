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

package audit

import config.MicroserviceAppConfig
import org.mockito.{ArgumentCaptor, Matchers}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class AuditServiceSpec extends WordSpec with MustMatchers with MockitoSugar with ScalaFutures {

  private val connector = mock[AuditConnector]
  private val appConfig = new MicroserviceAppConfig(Configuration(), Environment.simple())

  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val service = new AuditServiceImpl(appConfig, connector)

  ".sendEvent" must {

    "send an audit event and return a success when it works" in {

      when(connector.sendExtendedEvent(any())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val eventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      whenReady(service.sendEvent(CTUTRSubmission("foo", "bar"))) {
        result =>

          verify(connector, times(1)).sendExtendedEvent(eventCaptor.capture())(any(), any())
          eventCaptor.getValue.auditSource mustBe "request-corporation-tax-number"
          eventCaptor.getValue.auditType mustBe "CTUTRSubmission"
          eventCaptor.getValue.detail mustBe Json.obj(
            "data" -> CTUTRSubmission(
              "foo", "bar"
            ),
            "ipAddress" -> "-",
            "Authorization" -> "-",
            "token" -> "-",
            "deviceID" -> "-"
          )
          eventCaptor.getValue.tags must contain(
            "transactionName" -> "CTUTRSubmission"
          )
          result mustEqual AuditResult.Success
      }
    }

    "return failure when the audit event fails" in {
      when(connector.sendEvent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(AuditResult.Failure("")))

      val result = service.sendEvent(CTUTRSubmission("foo", "bar"))

      result onComplete {
        case Success(x) => x mustBe AuditResult.Failure
        case Failure(_) => fail("submitEnrolment returned a failure")
      }
    }
  }
}
