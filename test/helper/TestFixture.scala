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

package helper

import audit.AuditService
import config.MicroserviceAppConfig
import connectors.DmsConnector
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.Mockito.{reset, mock => classMock}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach, Succeeded}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.{Injector, bind}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.stubControllerComponents
import play.api.test.StubPlayBodyParsersFactory
import services.{PdfGeneratorService, SubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

trait TestFixture
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with StubPlayBodyParsersFactory
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStubBehaviour)
  }

  val mockStubBehaviour: StubBehaviour = classMock(classOf[StubBehaviour])
  val stubBackendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

  val app: Application = new GuiceApplicationBuilder()
    .configure("internal-auth-token-initialiser.enabled" -> "false")
    .overrides(
      bind[BackendAuthComponents].toInstance(stubBackendAuthComponents)
    )
    .build()

  val injector: Injector = app.injector
  val appConfig : MicroserviceAppConfig = real[MicroserviceAppConfig]

  def real[T: ClassTag]: T = injector.instanceOf[T]

  implicit val materializer: Materializer = app.materializer
  implicit val as: ActorSystem = ActorSystem()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  val stubCC: ControllerComponents = stubControllerComponents(playBodyParsers = stubPlayBodyParsers(materializer))
  
  val mockWsClient: WSClient = mock[WSClient]
  val mockWsRequest: WSRequest = mock[WSRequest]

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val mockPdfService: PdfGeneratorService = mock[PdfGeneratorService]
  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockDmsConnector: DmsConnector = mock[DmsConnector]

  /**
   * Wraps some text extracted from an XML element to provide extra assertion methods
   *
   * @param text The text to implicitly wrap
   */
  implicit class XmlTextWrapper(val text:String) {
    def mustMatchDateTimeFormat(pattern: String): Assertion = {
      LocalDateTime.parse(this.text, DateTimeFormatter.ofPattern(pattern))
      Succeeded
    }
  }

}
