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

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.writeableOf_JsValue
import org.apache.pekko.actor.ActorSystem

sealed abstract class Done
object Done extends Done

abstract class InternalAuthTokenInitialiser {
  def initialised(dmsSubmissionAttachmentGrantsToken: UUID): Future[Done]
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject() extends InternalAuthTokenInitialiser {
  override def initialised(dmsSubmissionAttachmentGrantsToken: UUID): Future[Done] = Future.successful(Done)
}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject() (
                                                   appConfig: MicroserviceAppConfig,
                                                   httpClient: HttpClientV2,
                                                 )(implicit ec: ExecutionContext)
  extends InternalAuthTokenInitialiser
    with Logging {

  override def initialised(dmsSubmissionAttachmentGrantsToken: UUID = UUID.randomUUID()): Future[Done] = setup(dmsSubmissionAttachmentGrantsToken)

  private def setup(dmsSubmissionAttachmentGrantsToken: UUID): Future[Done] = for {
    _ <- ensureAuthToken()
    _ <- addDmsSubmissionAttachmentGrants(dmsSubmissionAttachmentGrantsToken)
  } yield Done

  private def ensureAuthToken(): Future[Done] =
    authTokenIsValid.flatMap { isValid =>
      if (isValid) {
        logger.info("[InternalAuthTokenInitialiser][ensureAuthToken] Auth token is already valid")
        Future.successful(Done)
      } else {
        createClientAuthToken()
      }
    }

  private def createClientAuthToken(): Future[Done] = {
    logger.info("[InternalAuthTokenInitialiser][createClientAuthToken] Initialising auth token")
    httpClient
      .post(url"${appConfig.internalAuthBaseUrl}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
          "token"       -> appConfig.authToken,
          "principal"   -> appConfig.appName,
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
      )
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == CREATED) {
          logger.info(
            "[InternalAuthTokenInitialiser][createClientAuthToken] Auth token initialised"
          )
          Future.successful(Done)
        } else {
          logger.error(
            s"[InternalAuthTokenInitialiser][createClientAuthToken] Unable to initialise internal-auth token. response.body : ${response.body}"
          )
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }
  }

  private def addDmsSubmissionAttachmentGrants(dmsSubmissionAttachmentGrantsToken: UUID): Future[Done] = {
    logger.info(
      "[InternalAuthTokenInitialiser][addDmsSubmissionsAttachmentGrants] Initialising dms-submission grants"
    )
    httpClient
      .post(url"${appConfig.internalAuthBaseUrl}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
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
      )
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == CREATED) {
          logger.info(
            "[InternalAuthTokenInitialiser][addDmsSubmissionAttachmentGrants] dms-submission grants added"
          )
          Future.successful(Done)
        } else {
          logger.error(
            "[InternalAuthTokenInitialiser][addDmsSubmissionAttachmentGrants] Unable to add dms-submission grants"
          )
          Future.failed(new RuntimeException("Unable to add dms-submission grants"))
        }
      }
  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("[InternalAuthTokenInitialiser][authTokenIsValid] Checking auth token")
    httpClient
      .get(url"${appConfig.internalAuthBaseUrl}/test-only/token")(HeaderCarrier())
      .setHeader("Authorization" -> appConfig.authToken)
      .execute[HttpResponse]
      .map(_.status == OK)
  }
}
