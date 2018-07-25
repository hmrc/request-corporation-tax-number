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

import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.pattern.Patterns.after
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import config.MicroserviceAppConfig
import model.Envelope
import model.domain.MimeContentType
import play.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class FileUploadConnector @Inject()(
                                     appConfig: MicroserviceAppConfig,
                                     val httpClient: HttpClient,
                                     val wsClient: WSClient,
                                     val metrics: Metrics
                                   )(implicit as: ActorSystem) extends HttpResponseHelper {

  def callbackUrl: String = appConfig.fileUploadCallbackUrl

  def fileUploadUrl: String = appConfig.fileUploadUrl

  def fileUploadFrontEndUrl: String = appConfig.fileUploadFrontendUrl

  def routingRequest(envelopeId: String): JsValue = Json.obj(
    "envelopeId" -> envelopeId,
    "application" -> "CTUTR",
    "destination" -> "DMS")

  def createEnvelopeBody: JsValue = Json.obj("callbackUrl" -> callbackUrl)

  def createEnvelope(implicit hc: HeaderCarrier): Future[String] = {

    val result = httpClient.POST[JsValue, HttpResponse](s"$fileUploadUrl/file-upload/envelopes", createEnvelopeBody).flatMap { response =>

      response.status match {
        case CREATED =>
          envelopeId(response).map(Future.successful).getOrElse {
            Future.failed(new RuntimeException("No envelope id returned by file upload service"))
          }
        case _ =>
          Future.failed(new RuntimeException(s"failed to create envelope with status [${response.status}]"))
      }
    }

    result.onFailure {
      case e =>
        Logger.error("[FileUploadConnector][createEnvelope] - call to create envelope failed", e)
    }

    result
  }

  def uploadFile(byteArray: Array[Byte], fileName: String, contentType: MimeContentType, envelopeId: String, fileId: String)
                (implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val multipartFormData = Source(FilePart("attachment", fileName, Some(contentType.description),
      Source(ByteString(byteArray) :: Nil)) :: DataPart("", "") :: Nil)

    val result = wsClient.url(s"$fileUploadFrontEndUrl/file-upload/upload/envelopes/$envelopeId/files/$fileId")
      .withHeaders(hc.copy(otherHeaders = Seq("CSRF-token" -> "nocheck")).headers: _*).post(multipartFormData).flatMap { response =>

      response.status match {
        case OK =>
          Future.successful(HttpResponse(response.status))
        case _ =>
          Future.failed(new RuntimeException(s"failed with status [${response.status}]"))
      }
    }

    result.onFailure {
      case e =>
        Logger.error("[FileUploadConnector][uploadFile] - call to upload file failed", e)
    }

    result
  }

  def closeEnvelope(envId: String)(implicit hc: HeaderCarrier): Future[String] = {

    val result = httpClient.POST[JsValue, HttpResponse](s"$fileUploadUrl/file-routing/requests", routingRequest(envId)).flatMap { response =>
      response.status match {
        case CREATED =>
          envelopeId(response).map(Future.successful).getOrElse {
            Future.failed(new RuntimeException("No routing id returned"))
          }
        case BAD_REQUEST =>
          if (response.body.contains("Routing request already received for envelope")) {
            Logger.warn(s"[FileUploadConnector][closeEnvelope] Routing request already received for envelope")
            Future.successful("Already Closed")
          } else {
            Future.failed(new RuntimeException("failed with status 400 bad request"))
          }
        case _ =>
          Future.failed(new RuntimeException(s"failed to close envelope with status [${response.status}]"))
      }
    }

    result.onFailure {
      case e =>
        Logger.error("[FileUploadConnector][closeEnvelope] call to close envelope failed", e)
    }

    result
  }

  def parseEnvelope(body: String): Future[Envelope] = {
    val envelope: JsResult[Envelope] = Json.parse(body).validate[Envelope]
    envelope match {
      case s: JsSuccess[Envelope] => Future.successful(s.get)
      case _ => Future.failed(new RuntimeException("Failed to parse envelope"))
    }
  }

  def retry(envelopeId: String, cur: Int, attempt: Int, factor: Float = 2f)(implicit hc: HeaderCarrier): Future[Envelope] = {
    attempt match {
      case attempt: Int if attempt < 5 =>
        val nextTry: Int = Math.ceil(cur * factor).toInt
        val nextAttempt = attempt + 1

        after(nextTry.milliseconds, as.scheduler, global, Future.successful(1)).flatMap { _ =>
          envelopeSummary(envelopeId, nextTry, nextAttempt)(as, hc)
        }
      case _ =>
        Future.failed(new RuntimeException(s"[FileUploadConnector][retry] envelope[$envelopeId] summary failed at attempt: $attempt"))
    }
  }

  def envelopeSummary(envelopeId: String, nextTry: Int = 10, attempt: Int = 1)(implicit as: ActorSystem, hc: HeaderCarrier): Future[Envelope] = {
    httpClient.GET(s"$fileUploadUrl/file-upload/envelopes/$envelopeId").flatMap {
      response =>
        response.status match {
          case OK =>
            parseEnvelope(response.body)
          case NOT_FOUND =>
            retry(envelopeId, nextTry, attempt)
          case _ =>
            Future.failed(new RuntimeException(s"[FileUploadConnector][envelopeSummary]Failed with status [${response.status}]"))
        }
    }
  }

  private def envelopeId(response: HttpResponse): Option[String] = {
    response.header("Location").map(path =>
      path.split("/")
        .reverse
        .head)
  }
}