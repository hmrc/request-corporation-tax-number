/*
 * Copyright 2021 HM Revenue & Customs
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

package helper

import akka.actor.ActorSystem
import akka.stream.Materializer
import audit.AuditService
import com.kenshoo.play.metrics.Metrics
import config.MicroserviceAppConfig
import connectors.{FileUploadConnector, PdfConnector}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.stubControllerComponents
import play.api.test.StubPlayBodyParsersFactory
import services.{FileUploadService, PdfService, SubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

trait TestFixture extends AnyWordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite with ScalaFutures with StubPlayBodyParsersFactory {
  val injector: Injector = app.injector
  val appConfig : MicroserviceAppConfig = real[MicroserviceAppConfig]

  def real[T: ClassTag]: T = injector.instanceOf[T]

  implicit val materializer: Materializer = app.materializer
  implicit val as: ActorSystem = ActorSystem()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy implicit val ec: ExecutionContext = real[ExecutionContext]
  val stubCC: ControllerComponents = stubControllerComponents(playBodyParsers = stubPlayBodyParsers(materializer))

  val mockHttpClient: DefaultHttpClient = mock[DefaultHttpClient]
  val mockMetrics: Metrics = mock[Metrics]

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockFileUploadConnector: FileUploadConnector = mock[FileUploadConnector]
  val mockPdfConnector: PdfConnector = mock[PdfConnector]

  val mockPdfService: PdfService = mock[PdfService]
  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockFileUploadService: FileUploadService = mock[FileUploadService]


}
