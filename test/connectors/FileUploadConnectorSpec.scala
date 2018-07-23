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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.SpecBase
import model.domain.MimeContentType
import org.scalacheck.{Gen, Shrink}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import util.WireMockHelper

class FileUploadConnectorSpec extends SpecBase with WireMockHelper with GuiceOneAppPerSuite with ScalaFutures with PropertyChecks with IntegrationPatience {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.file-upload.port" -> server.port
      )
      .build()

  implicit val hc = HeaderCarrier()

  private lazy val connector: FileUploadConnector =
    app.injector.instanceOf[FileUploadConnector]

  private val statuses: Gen[Int] =
    Gen.chooseNum(
      200, 599,
      400, 499, 500
    )

  private val uuid: Gen[String] = Gen.uuid.map(_.toString)


  "createEnvelope" must {
    "return an envelope id" in {
      forAll(uuid) {
        (envId) =>
          server.stubFor(
            post(urlEqualTo("/file-upload/envelopes"))
              .willReturn(
                aResponse()
                  .withHeader("Location", s"file-upload/envelope/$envId")
                  .withStatus(Status.CREATED)
              )
          )

          whenReady(connector.createEnvelope) {
            result =>
              result mustBe envId
          }
      }
    }

    "return exceptions" when {
      "no location header provided" in {
        server.stubFor(
          post(urlEqualTo("/file-upload/envelopes"))
            .willReturn(
              aResponse()
                .withStatus(Status.CREATED)
            )
        )

        whenReady(connector.createEnvelope.failed) {
          exception =>
            exception.getMessage mustBe "No envelope id returned by file upload service"
        }
      }

      "status not created(201)" in {
        forAll(statuses) {
          (returnStatus) =>
            server.stubFor(
              post(urlEqualTo("/file-upload/envelopes"))
                .willReturn(
                  status(returnStatus)
                )
            )

            whenever(returnStatus != 201) {
              whenReady(connector.createEnvelope.failed) {
                exception =>
                  exception.getMessage mustBe s"failed to create envelope with status [$returnStatus]"
              }
            }
        }
      }
    }
  }

  "uploadFile" must {
    "return Success" when {
      "File upload service successfully uploads the file" in {
        forAll(uuid, uuid) {
          (envId, fileId) =>
            server.stubFor(
              post(urlEqualTo(s"/file-upload/upload/envelopes/$envId/files/$fileId"))
                .willReturn(
                  status(Status.OK)
                )
            )

            whenReady(connector.uploadFile(new Array[Byte](1), "fileName.pdf", MimeContentType.ApplicationPdf, envId, fileId)) {
              result =>
                result.status mustBe Status.OK
            }
        }
      }
    }

    "return Exception" when {
      "File upload status not OK(200)" in {
        forAll(statuses, uuid, uuid) {
          (returnStatus, envId, fileId) =>
            server.stubFor(
              post(urlEqualTo(s"/file-upload/upload/envelopes/$envId/files/$fileId"))
                .willReturn(
                  status(returnStatus)
                )
            )

            whenever(returnStatus != 200) {
              whenReady(connector.uploadFile(new Array[Byte](1), "fileName.pdf", MimeContentType.ApplicationPdf, envId, fileId).failed) {
                exception =>
                  exception.getMessage mustBe s"failed with status [$returnStatus]"
              }
            }
        }
      }
    }
  }


  "closeEnvelope" must {
    "return a routed Id" in {
      forAll(uuid, uuid) {
        (envId, routingId) =>
          server.stubFor(
            post(urlEqualTo("/file-routing/requests"))
              .willReturn(
                aResponse()
                  .withHeader("Location", s"/file-routing/requests/$routingId")
                  .withStatus(Status.CREATED)
              )
          )

          whenReady(connector.closeEnvelope(envId)) {
            result =>
              result mustBe routingId
          }
      }
    }

    "return already closed message if routing request already received" in {
      forAll(uuid) {
        (envId) =>
          server.stubFor(
            post(urlEqualTo("/file-routing/requests"))
              .willReturn(
                aResponse()
                  .withStatus(Status.BAD_REQUEST)
                  .withBody("""{"error":{"msg":"Routing request already received for envelope: 9cd81d3c-75bf-4069-9f0c-ec2b3c3fe1cf"}}""")
              )
          )

          whenReady(connector.closeEnvelope(envId)) {
            result =>
              result mustBe "Already Closed"
          }
      }
    }

    "return exceptions" when {
      "no location header provided" in {
        forAll(uuid, uuid) {
          (envId, routingId) =>
            server.stubFor(
              post(urlEqualTo("/file-routing/requests"))
                .willReturn(
                  aResponse()
                    .withStatus(Status.CREATED)
                )
            )

            whenReady(connector.closeEnvelope(envId).failed) {
              exception =>
                exception.getMessage mustBe "No routing id returned"
            }
        }
      }

      "File upload status not CREATED(201) OR BAD_REQUEST(400)" in {
        forAll(statuses, uuid) {
          (returnStatus, envId) =>
            server.stubFor(
              post(urlEqualTo("/file-routing/requests"))
                .willReturn(
                  status(returnStatus)
                )
            )

            whenever(returnStatus != 201 && returnStatus != 400) {
              whenReady(connector.closeEnvelope(envId).failed) {
                exception =>
                  exception.getMessage mustBe s"failed to close envelope with status [$returnStatus]"
              }
            }
        }
      }

      "File upload status BAD_REQUEST(400) and not already received routing request" in {
        forAll(uuid) {
          (envId) =>
            server.stubFor(
              post(urlEqualTo("/file-routing/requests"))
                .willReturn(
                  status(Status.BAD_REQUEST)
                )
            )

            whenReady(connector.closeEnvelope(envId).failed) {
              exception =>
                exception.getMessage mustBe "failed with status 400 bad request"
            }

        }
      }
    }
  }


}



//  "envelopeSummary" must {
//    "return an envelope" in {
//      val sut = createSut
//
//      Await.result(sut.envelopeSummary(envelopeId), 5.seconds) mustBe Envelope(envelopeId,"http://callback","OPEN",Seq(File(fileName,"AVAILABLE")))
//    }
//    "throw error on failed GET" in {
//      val sut = createSut
//
//      when(sut.httpClient.GET[Envelope](any())(any(), any(), any()))
//        .thenReturn(Future.failed(new RuntimeException("Call failed")))
//
//      val ex = the[RuntimeException] thrownBy Await.result(sut.envelopeSummary(envelopeId), 5 seconds)
//
//      ex.getMessage mustBe "Call failed"
//    }
//  }
//
//  private def createMockResponse(status: Int, body: String): WSResponse = {
//    val wsResponseMock = mock[WSResponse]
//    when(wsResponseMock.status).thenReturn(status)
//    when(wsResponseMock.body).thenReturn(body)
//    wsResponseMock
//  }
//
//  def createSut = new SUT
//
//  private val envelopeId: String = "0b215e97-11d4-4006-91db-c067e74fc653"
//  private val fileId = "fileId"
//  private val fileName = "fileName.pdf"
//  private val contentType = MimeContentType.ApplicationPdf
//
//  val mockHttp = mock[HttpClient]
//  val mockClient = mock[WSClient]
//  val metrics = mock[Metrics]
//
//  class SUT extends FileUploadConnector(
//    appConfig,
//    mockHttp,
//    mockClient,
//    metrics
//  ) {
//
//    when(mockHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
//      .thenReturn(Future.successful(HttpResponse(201, None,
//        Map("Location" -> Seq(s"localhost:8898/file-upload/envelopes/$envelopeId")))))
//
//    when(mockHttp.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
//      .thenReturn(Future.successful(HttpResponse(201, None,
//        Map("Location" -> Seq(s"/file-routing/requests/$envelopeId")))))
//
//    when(mockHttp.GET[Envelope](any())(any(), any(), any()))
//      .thenReturn(Future.successful(Envelope(envelopeId,"http://callback","OPEN",Seq(File(fileName,"AVAILABLE")))))
//
//    override val fileUploadUrl: String = "file-upload"
//
//    override val fileUploadFrontEndUrl: String = "file-upload-frontend"
//
//    override val callbackUrl: String = "http://callback"
//
//  }
