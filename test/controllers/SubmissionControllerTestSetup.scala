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

import config.MicroserviceAppConfig
import helper.TestFixture
import model.domain.SubmissionResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

case class SubmissionControllerTestSetup(storeSubmissionEnabled: Boolean) extends TestFixture {

  val servicesConfig: ServicesConfig = mock[ServicesConfig]
  val appConfigWithMockedServiceConfig = new MicroserviceAppConfig(servicesConfig)

  val submissionController: SubmissionController =
    new SubmissionController(mockMongoSubmissionService, mockSubmissionService, mockSubmissionMongoRepository, mockAuditService, appConfigWithMockedServiceConfig, stubCC)

  when(servicesConfig.getBoolean(eqTo("mongodb.store-submission-enabled"))).thenReturn(storeSubmissionEnabled)

  when(mockSubmissionService.submit(any(), any())(any()))
    .thenReturn(Future.successful(SubmissionResponse("12345", "12345-SubmissionCTUTR-20171023-iform.pdf")))
  when(mockAuditService.sendEvent(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

}
