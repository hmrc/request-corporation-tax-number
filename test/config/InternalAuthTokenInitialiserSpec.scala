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

package config

import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import util.WireMockHelper
import play.api.libs.json.{JsObject, Json}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{AUTHORIZATION, running}

import java.util.UUID

class InternalAuthTokenInitialiserSpec
  extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with WireMockHelper
    with Matchers {

  val authToken: String = "authToken"
  val appName: String = "request-corporation-tax-number"
  val dmsSubmissionAttachmentGrantsToken: UUID = UUID.fromString("73e63d8d-99c0-472f-bf38-e0f38c05d4aa")

  def buildApp(authTokenInitialised: Boolean): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.internal-auth.port" -> server.port(),
      "appName" -> appName,
      "internal-auth-token-initialiser.enabled" -> authTokenInitialised,
      "internal-auth.token" -> authToken
    )
    .build()

  val createClientAuthTokenJsonRequest: JsObject = Json.obj(
    "token" -> authToken,
    "principal" -> appName,
    "permissions" -> Seq(
      Json.obj(
        "resourceType"     -> "dms-submission",
        "resourceLocation" -> "submit",
        "actions"          -> List("WRITE")
      ),
      Json.obj(
        "resourceType"     -> "object-store",
        "resourceLocation" -> "request-corporation-tax-number",
        "actions"          -> List("READ", "WRITE")
      )
    )
  )

  def addDmsSubmissionAttachmentGrantsJsonRequest(dmsSubmissionAttachmentGrantsToken: UUID): JsObject = Json.obj(
    "token"       -> dmsSubmissionAttachmentGrantsToken,
    "principal"   -> "dms-submission",
    "permissions" -> Seq(
      Json.obj(
        "resourceType"     -> "request-corporation-tax-number",
        "resourceLocation" -> "dms/callback",
        "actions"          -> List("WRITE")
      )
    )
  )

  def stubGetAuthToken(response: Int): StubMapping =
    server.stubFor(
      get(urlMatching("/test-only/token"))
        .willReturn(aResponse().withStatus(response))
    )

  def stubCreateClientAuthToken(response: Int): StubMapping =
    server.stubFor(
      post(urlMatching("/test-only/token"))
        .willReturn(
          aResponse()
            .withStatus(response)
        )
        .withRequestBody(
          equalToJson(Json.stringify(Json.toJson(createClientAuthTokenJsonRequest)))
        )
    )

  def stubAddDmsSubmissionAttachmentGrants(response: Int): StubMapping =
    server.stubFor(
      post(urlMatching("/test-only/token"))
        .willReturn(
          aResponse()
            .withStatus(response)
        )
        .withRequestBody(
          equalToJson(Json.stringify(Json.toJson(addDmsSubmissionAttachmentGrantsJsonRequest(dmsSubmissionAttachmentGrantsToken))))
        )
    )

  "when configured to run" must {

    "initialise the internal-auth token if it is not already initialised" in {

      stubGetAuthToken(NOT_FOUND)
      stubCreateClientAuthToken(CREATED)
      stubAddDmsSubmissionAttachmentGrants(CREATED)

      val app = buildApp(true)

      running(app) {
        app.injector.instanceOf[InternalAuthTokenInitialiser].initialised(dmsSubmissionAttachmentGrantsToken).futureValue
        eventually(Timeout(Span(30, Seconds))) {
          server.verify(1,
            getRequestedFor(urlMatching("/test-only/token"))
              .withHeader(AUTHORIZATION, equalTo(authToken))
          )
          server.verify(1,
            postRequestedFor(urlMatching("/test-only/token"))
              .withRequestBody(
                equalToJson(Json.stringify(Json.toJson(createClientAuthTokenJsonRequest)))
              )
          )
          server.verify(1,
            postRequestedFor(urlMatching("/test-only/token"))
              .withRequestBody(
                equalToJson(Json.stringify(Json.toJson(addDmsSubmissionAttachmentGrantsJsonRequest(dmsSubmissionAttachmentGrantsToken))))
              )
          )
        }
      }
    }

    "not initialise the internal-auth token if it is already initialised" in {

      stubGetAuthToken(OK)
      stubAddDmsSubmissionAttachmentGrants(CREATED)

      val app = buildApp(true)

      running(app) {

        app.injector.instanceOf[InternalAuthTokenInitialiser].initialised(dmsSubmissionAttachmentGrantsToken).futureValue

        server.verify(1,
          getRequestedFor(urlMatching("/test-only/token"))
            .withHeader(AUTHORIZATION, equalTo(authToken))
        )
        server.verify(0,
          postRequestedFor(urlMatching("/test-only/token"))
            .withRequestBody(equalToJson(Json.stringify(Json.toJson(createClientAuthTokenJsonRequest))))
        )
        server.verify(1,
          postRequestedFor(urlMatching("/test-only/token"))
            .withRequestBody(equalToJson(
              Json.stringify(Json.toJson(addDmsSubmissionAttachmentGrantsJsonRequest(dmsSubmissionAttachmentGrantsToken)))
            ))
        )
      }
    }

  }

  "when not configured to run" must {

    "not make the relevant calls to internal-auth" in {

      val app = buildApp(false)

      app.injector.instanceOf[InternalAuthTokenInitialiser].initialised(UUID.randomUUID()).futureValue

      server.verify(0,
        getRequestedFor(urlMatching("/test-only/token"))
      )
      server.verify(0,
        postRequestedFor(urlMatching("/test-only/token"))
      )
    }
  }
}
