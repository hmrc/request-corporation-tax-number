/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import config.MicroserviceAppConfig
import model.domain.MimeContentType
import model.{Envelope, File}
import org.scalacheck.{Gen, Shrink}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.http.HeaderCarrier
import util.WireMockHelper

class FileUploadConnectorSpec extends PlaySpec with WireMockHelper with ScalaFutures with PropertyChecks with IntegrationPatience {

  def injector: Injector = app.injector

  def appConfig : MicroserviceAppConfig = injector.instanceOf[MicroserviceAppConfig]

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  private val as = ActorSystem()

  implicit val hc = HeaderCarrier()

  private lazy val connector: FileUploadConnector =
    app.injector.instanceOf[FileUploadConnector]

  private val statuses: Gen[Int] =
    Gen.chooseNum(
      200, 599,400, 499, 500
    )

  private val uuid: Gen[String] = Gen.uuid.map(_.toString)

  private val envelopeStatuses: Gen[String] = Gen.oneOf("OPEN", "CLOSED", "SEALED", "DELETED")

  private val fileStatuses: Gen[String] = Gen.oneOf("AVAILABLE", "QUARANTINED", "CLEANED", "INFECTED")
  private val file = for {
    name <- uuid
    status <- fileStatuses
  } yield {
    File(name, status)
  }
  private val files: Gen[Seq[File]] = Gen.listOf(file)


  "createEnvelope" must {
    "return an envelope id" in {
      forAll(uuid) {
        envId =>
          WireMock.stubFor(
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
        WireMock.stubFor(
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
          returnStatus =>
            WireMock.stubFor(
              post(urlEqualTo("/file-upload/envelopes"))
                .willReturn(
                  status(returnStatus)
                )
            )

            whenever(returnStatus != 201) {
              whenReady(connector.createEnvelope.failed) {
                exception =>
                  exception mustBe a[RuntimeException]
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
            WireMock.stubFor(
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
            WireMock.stubFor(
              post(urlEqualTo(s"/file-upload/upload/envelopes/$envId/files/$fileId"))
                .willReturn(
                  status(returnStatus)
                )
            )

            whenever(returnStatus != 200) {
              whenReady(connector.uploadFile(new Array[Byte](1), "fileName.pdf", MimeContentType.ApplicationPdf, envId, fileId).failed) {
                exception =>
                  exception mustBe a[RuntimeException]
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
          WireMock.stubFor(
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
        envId =>
          WireMock.stubFor(
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
            WireMock.stubFor(
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
            WireMock.stubFor(
              post(urlEqualTo("/file-routing/requests"))
                .willReturn(
                  status(returnStatus)
                )
            )

            whenever(returnStatus != 201 && returnStatus != 400) {
              whenReady(connector.closeEnvelope(envId).failed) {
                exception =>
                  exception mustBe a[RuntimeException]
              }
            }
        }
      }

      "File upload status BAD_REQUEST(400) and not already received routing request" in {
        forAll(uuid) {
          envId =>
            WireMock.stubFor(
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


  "envelopeSummary and retry" when {
    "OK Response" must {
      "return an envelope with no files" in {
        forAll(uuid, envelopeStatuses) {
          (envId, envelopeStatus) =>
            WireMock.stubFor(
              get(urlEqualTo(s"/file-upload/envelopes/$envId"))
                .willReturn(
                  aResponse()
                    .withStatus(Status.OK)
                    .withBody(
                      s"""{"id": "$envId", "status": "$envelopeStatus"}"""
                    )
                )
            )

            whenReady(connector.envelopeSummary(envId)(as, hc)) {
              result =>
                result mustBe Envelope(envId, None, envelopeStatus, None)
            }
        }
      }

      "return an envelope with some files" in {
        forAll(uuid, envelopeStatuses, files) {
          (envId, envelopeStatus, files) =>
            WireMock.stubFor(
              get(urlEqualTo(s"/file-upload/envelopes/$envId"))
                .willReturn(
                  aResponse()
                    .withStatus(Status.OK)
                    .withBody(
                      Json.obj(
                        "id" -> envId,
                        "status" -> envelopeStatus,
                        "files" -> JsArray(
                          files.map(file =>
                            Json.obj(
                              "name" -> file.name,
                              "status" -> file.status)
                          )
                        )
                      ).toString()
                    )
                )
            )

            whenever(files.nonEmpty) {
              whenReady(connector.envelopeSummary(envId)(as, hc)) {
                result =>
                  result mustBe Envelope(envId, None, envelopeStatus, Some(files))
              }
            }
        }
      }
    }

    "NOT_FOUND(404) response" must {
      "return envelope on retry" in {
        forAll(uuid, envelopeStatuses) {
          (envId, envelopeStatus) =>
            WireMock.stubFor(
              get(urlEqualTo(s"/file-upload/envelopes/$envId"))
                .willReturn(
                  aResponse()
                    .withStatus(Status.OK)
                    .withBody(
                      Json.obj(
                        "id" -> envId,
                        "status" -> envelopeStatus
                      ).toString()
                    )
                )
            )

            whenReady(connector.retry(envId, 10, 2)) {
              result =>
                result mustBe Envelope(envId, None, envelopeStatus, None)
            }
        }
      }

      "return exception on 5th attempt" in {
        forAll(uuid) {
          (envId) =>
            WireMock.stubFor(
              get(urlEqualTo(s"/file-upload/envelopes/$envId"))
                .willReturn(
                  aResponse()
                    .withStatus(Status.NOT_FOUND)
                )
            )

            whenReady(connector.retry(envId, 10, 1).failed) {
              exception =>
                exception.getMessage mustBe s"[FileUploadConnector][retry] envelope[$envId] summary failed at attempt: 5"
            }
        }
      }
    }


    "File upload status not OK(200) or NOT_FOUND(404)" must {
      "Return Exception" in {
        forAll(statuses, uuid) {
          (returnStatus, envId) =>
            WireMock.stubFor(
              get(urlEqualTo(s"/file-upload/envelopes/$envId"))
                .willReturn(
                  status(returnStatus)
                )
            )

            whenever(returnStatus != Status.OK && returnStatus != Status.NOT_FOUND) {
              whenReady(connector.envelopeSummary(envId)(as, hc).failed) {
                exception =>
                  exception mustBe a[RuntimeException]
              }
            }
        }
      }
    }
  }

  "parseEnvelope" must {
    "return an envelope on success" in {
      forAll(uuid, envelopeStatuses, files) {
        (envId, envelopeStatus, files) =>
          val body = Json.obj(
            "id" -> envId,
            "status" -> envelopeStatus,
            "files" -> JsArray(
              files.map(file =>
                Json.obj(
                  "name" -> file.name,
                  "status" -> file.status)
              )
            )
          ).toString()

          whenReady(connector.parseEnvelope(body)) {
            result =>
              result mustBe Envelope(envId, None, envelopeStatus, Some(files))
          }
      }
    }

    "throw exception on failure to parse" in {
      val invalidBody = """{"invalid":"json"}"""
      whenReady(connector.parseEnvelope(invalidBody).failed) {
        exception =>
          exception.getMessage mustBe s"Failed to parse envelope"
      }
    }
  }

}
