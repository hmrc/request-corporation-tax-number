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

package connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.MicroserviceAppConfig
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import org.apache.pekko.util.ByteString
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{AUTHORIZATION, USER_AGENT}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.WireMockHelper

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DmsConnectorSpec
  extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with WireMockHelper
    with Matchers {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val today: LocalDate = LocalDate.now()
  val formatToday: String = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
  val pdfFileName = "CTUTR_example_04102024.pdf"
  val roboticXmlFileName = "OMAUM9YP0R5G-SubmissionCTUTR-20250213-robotic.xml"

  val url = "/dms-submission/submit"
  val dateOfReceipt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
    LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
  )

  val pdf: Array[Byte] = "Some string of data".getBytes
  val roboticXml: Array[Byte] = "Another string of data".getBytes

  private lazy val connector: DmsConnector = app.injector.instanceOf[DmsConnector]

  val appConfig = new MicroserviceAppConfig(new ServicesConfig(app.configuration))
  val ctutrMetadata: CTUTRMetadata = CTUTRMetadata(appConfig)

  def defineDmsSubStub(ctutrMetadata: CTUTRMetadata, response: ResponseDefinitionBuilder): StubMapping =
    server.stubFor(
      post(urlMatching(url))
        .withMultipartRequestBody(aMultipart().withName("submissionReference").withBody(containing(ctutrMetadata.submissionReference)))
        .withMultipartRequestBody(aMultipart().withName("callbackUrl")
          .withBody(containing("https://localhost:9201/request-corporation-tax-number/dms-submission/callback")))
        .withMultipartRequestBody(aMultipart().withName("metadata.store").withBody(containing("true")))
        .withMultipartRequestBody(aMultipart().withName("metadata.source").withBody(containing("CTUTR")))
        .withMultipartRequestBody(aMultipart().withName("metadata.timeOfReceipt").withBody(containing(dateOfReceipt)))
        .withMultipartRequestBody(aMultipart().withName("metadata.formId").withBody(containing("CTUTR")))
        .withMultipartRequestBody(aMultipart().withName("metadata.customerId").withBody(containing("")))
        .withMultipartRequestBody(aMultipart().withName("metadata.casKey").withBody(containing("")))
        .withMultipartRequestBody(
          aMultipart().withName("metadata.classificationType").withBody(containing("BT-CTS-CT UTR"))
        )
        .withMultipartRequestBody(aMultipart().withName("metadata.businessArea").withBody(containing("BT")))
        .withMultipartRequestBody(aMultipart().withName("form").withBody(binaryEqualTo(pdf)))
        .withMultipartRequestBody(aMultipart().withName("attachment").withBody(binaryEqualTo(roboticXml)))
        .withHeader(AUTHORIZATION, containing(ctutrMetadata.appConfig.authToken))
        .withHeader(USER_AGENT, containing("request-corporation-tax-number"))
        .willReturn(response)
    )

  "postFileData" must {

    "return a successful future when the store responds with ACCEPTED and a SubmissionResponse.Success" in {

      val submissionResponse: SubmissionResponse = SubmissionResponse(ctutrMetadata.submissionReference, pdfFileName)
      val stubbedResponse: ResponseDefinitionBuilder =
        aResponse()
          .withStatus(ACCEPTED)
          .withBody(Json.toJson(submissionResponse).toString)

      defineDmsSubStub(ctutrMetadata, stubbedResponse)

      val response: SubmissionResponse = connector.postFileData(
        ctutrMetadata = ctutrMetadata,
        pdf = ByteString(pdf),
        pdfFileName = pdfFileName,
        robotXml = ByteString(roboticXml),
        robotXmlFileName = roboticXmlFileName,
        dateOfReceipt = dateOfReceipt
      ).futureValue

      response mustEqual submissionResponse
    }

    "return a RuntimeException" when {
      Seq(
        BAD_REQUEST,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND
      ).foreach { returnStatus: Int =>
        s"the call to DMS Submissions fails returning $returnStatus" in {

          val stubbedResponse: ResponseDefinitionBuilder =
            aResponse()
              .withStatus(returnStatus)
              .withBody(s"Something went wrong [$returnStatus]")

          defineDmsSubStub(ctutrMetadata, stubbedResponse)

          val response: Future[Throwable] = connector.postFileData(
            ctutrMetadata = ctutrMetadata,
            pdf = ByteString(pdf),
            pdfFileName = pdfFileName,
            robotXml = ByteString(roboticXml),
            robotXmlFileName = roboticXmlFileName,
            dateOfReceipt = dateOfReceipt
          ).failed

          val expectedErrorMessage = s"Failed with status [$returnStatus]"
          whenReady(response) {
            result: Throwable =>
              result mustBe a[RuntimeException]
              result.getMessage mustBe expectedErrorMessage
          }
        }
      }
    }
  }
}
