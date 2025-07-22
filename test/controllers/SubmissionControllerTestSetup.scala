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
import model.{CompanyDetails, Submission}
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import org.mockito.ArgumentMatchers.{any, argThat, eq => eqTo}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Clock, Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class SubmissionControllerTestSetup(storeSubmissionEnabled: Boolean) extends TestFixture {

  val servicesConfig: ServicesConfig = mock[ServicesConfig]
  val appConfigWithMockedServiceConfig = new MicroserviceAppConfig(servicesConfig)

  val fixedClock: Clock = Clock.fixed(Instant.parse("2024-10-04T12:17:18Z"), ZoneOffset.UTC)

  val submissionController: SubmissionController =
    new SubmissionController(
      mockMongoSubmissionService,
      mockSubmissionService,
      mockSubmissionMongoRepository,
      fixedClock,
      mockAuditService,
      appConfigWithMockedServiceConfig,
      stubCC
    )

  val createdAt: LocalDateTime = LocalDateTime.parse("Friday 04 October 2024 12:17:18", DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy HH:mm:ss"))
  val validSubmission: Submission = Submission(companyDetails = CompanyDetails("Big Company", "AB123123"))
  val expectedCTUTRMetadata: CTUTRMetadata = CTUTRMetadata(appConfig, "AB123123", createdAt)

  when(servicesConfig.getBoolean(eqTo("submission.save-to-db"))).thenReturn(storeSubmissionEnabled)

  when(mockSubmissionService.submit(eqTo(validSubmission), any())(any()))
    .thenReturn(Future.successful(SubmissionResponse("12345", "12345-SubmissionCTUTR-20171023-iform.pdf")))
  when(mockAuditService.sendEvent(any())(any(), any(), any())).thenReturn(Future.successful(AuditResult.Success))

  val validDataset: JsValue = Json.parse(
    """
      |{
      |   "companyDetails": {
      |     "companyName": "Big Company",
      |     "companyReferenceNumber": "AB123123"
      |   }
      |}
      |""".stripMargin)


  val invalidDataset: JsValue = Json.parse(
    """
      |{
      |   "companyDetails": {
      |     "company": "Bad Company",
      |     "reference": "XX123123"
      |   }
      |}
      |""".stripMargin)

  val fakeRequestValidDataset: FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/submit").withJsonBody(validDataset)

  val fakeRequestBadRequest: FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/submit").withJsonBody(invalidDataset)

  def stubSuccessfulStoreSubmission(objectId: String): OngoingStubbing[Future[String]] =
    when(mockMongoSubmissionService.storeSubmission(
      eqTo(validSubmission),
      argThat { metadata: CTUTRMetadata =>
        metadata.customerId == expectedCTUTRMetadata.customerId &&
          metadata.createdAt == expectedCTUTRMetadata.createdAt
      }
    )).thenReturn(Future.successful(objectId))

  def stubFailedStoreSubmission(exception: Exception): OngoingStubbing[Future[String]] =
    when(mockMongoSubmissionService.storeSubmission(
      eqTo(validSubmission),
      argThat { metadata: CTUTRMetadata =>
        metadata.customerId == expectedCTUTRMetadata.customerId &&
          metadata.createdAt == expectedCTUTRMetadata.createdAt
      }
    )).thenReturn(Future.failed(exception))
}
