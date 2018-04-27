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

import audit.{AuditEvent, AuditService}
import config.MicroserviceAppConfig
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsString, Writes}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class AuditServiceSpec extends WordSpec with MustMatchers with MockitoSugar with ScalaFutures {

  private val connector = mock[AuditConnector]
  private val appConfig = new MicroserviceAppConfig(Configuration(), Environment.simple())

  object TestEvent extends AuditEvent {

    override val auditType: String = "test"

    implicit val writes: Writes[TestEvent.type] =
      Writes {
        _ =>
          JsString("test-data")
      }
  }

  val service = new AuditService(appConfig, connector)

  ".sendEvent" must {

    "return an AuditSuccess" in {

      whenReady(service.sendEvent(TestEvent)) {
        result =>

      }
    }
  }
}
