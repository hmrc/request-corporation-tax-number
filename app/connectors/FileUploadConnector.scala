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

package connectors

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.Patterns.after
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import com.google.inject.Inject
import config.MicroserviceAppConfig
import model.Envelope
import model.domain.MimeContentType
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpReads, HttpResponse, StringContextOps}

import javax.inject.Singleton
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadConnector @Inject()(appConfig: MicroserviceAppConfig,
                                    val httpClientV2: HttpClientV2,
                                    val wsClient: WSClient,
                                    implicit val ec: ExecutionContext
                                   )(implicit as: ActorSystem) extends Logging {

  private val callbackUrl: String = appConfig.fileUploadCallbackUrl
  private val fileUploadUrl: String = appConfig.fileUploadUrl
  private val fileUploadFrontEndUrl: String = appConfig.fileUploadFrontendUrl

  private val firstRetryMilliseconds: Int = 20
  private val maxAttemptNumber: Int = 5

  implicit val httpReads: HttpReads[HttpResponse] = (_: String, _: String, response: HttpResponse) => response


  private def routingRequest(envelopeId: String): JsValue = Json.obj(
    "envelopeId" -> envelopeId,
    "application" -> "CTUTR",
    "destination" -> "DMS"
  )

  private def createEnvelopeBody: JsValue = Json.obj("callbackUrl" -> callbackUrl)

  def createEnvelope(implicit hc: HeaderCarrier): Future[String] = {
    
    val result: Future[String] = httpClientV2
      .post(url"$fileUploadUrl/file-upload/envelopes")
      .withBody(createEnvelopeBody)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case CREATED =>
            envelopeId(response).map(Future.successful).getOrElse {
              Future.failed(new RuntimeException("No envelope id returned by file upload service"))
            }
          case _ =>
            Future.failed(new RuntimeException(s"failed to create envelope with status [${response.status}]"))
        }
      }

    result.failed.foreach { e =>
      logger.error("[FileUploadConnector][createEnvelope] - call to create envelope failed", e)
    }

    result
  }

  def uploadFile(byteArray: Array[Byte], fileName: String, contentType: MimeContentType, envelopeId: String, fileId: String)
                (implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val multipartFormData = Source(FilePart("attachment", fileName, Some(contentType.description),
      Source(ByteString(byteArray) :: Nil)) :: DataPart("", "") :: Nil)

    val headers: Seq[(String, String)] = {
      List(
        hc.requestId.map(rid => HeaderNames.xRequestId -> rid.value),
        hc.sessionId.map(sid => HeaderNames.xSessionId -> sid.value),
        hc.forwarded.map(f => HeaderNames.xForwardedFor -> f.value),
        Some(HeaderNames.xRequestChain -> hc.requestChain.value),
        hc.authorization.map(auth => HeaderNames.authorisation -> auth.value),
        hc.trueClientIp.map(HeaderNames.trueClientIp -> _),
        hc.trueClientPort.map(HeaderNames.trueClientPort -> _),
        hc.gaToken.map(HeaderNames.googleAnalyticTokenId -> _),
        hc.gaUserId.map(HeaderNames.googleAnalyticUserId -> _),
        hc.deviceID.map(HeaderNames.deviceID -> _),
        hc.akamaiReputation.map(HeaderNames.akamaiReputation -> _.value)
      ).flatten ++ Seq("CSRF-token" -> "nocheck") ++ hc.extraHeaders

    }

    val result: Future[HttpResponse] = wsClient.url(s"$fileUploadFrontEndUrl/file-upload/upload/envelopes/$envelopeId/files/$fileId")
      .withHttpHeaders(headers: _*).post(multipartFormData).flatMap { response =>

      response.status match {
        case OK =>
          Future.successful(HttpResponse(response.status, ""))
        case _ =>
          Future.failed(new RuntimeException(s"failed with status [${response.status}]"))
      }
    }

    result.failed.foreach { e =>
      logger.error("[FileUploadConnector][uploadFile] - call to upload file failed", e)
    }

    result
  }

  def closeEnvelope(envId: String)(implicit hc: HeaderCarrier): Future[String] = {

    val result = httpClientV2.post(url"$fileUploadUrl/file-routing/requests").withBody(routingRequest(envId)).execute[HttpResponse].flatMap { response =>
      response.status match {
        case CREATED =>
          envelopeId(response).map(Future.successful).getOrElse {
            Future.failed(new RuntimeException("No routing id returned"))
          }
        case BAD_REQUEST =>
          if (response.body.contains("Routing request already received for envelope")) {
            logger.warn(s"[FileUploadConnector][closeEnvelope] Routing request already received for envelope")
            Future.successful("Already Closed")
          } else {
            Future.failed(new RuntimeException("failed with status 400 bad request"))
          }
        case _ =>
          Future.failed(new RuntimeException(s"failed to close envelope with status [${response.status}]"))
      }
    }

    result.failed.foreach { e =>
      logger.error("[FileUploadConnector][closeEnvelope] call to close envelope failed", e)
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
      case attempt: Int if attempt < maxAttemptNumber =>
        val nextTry: Int = Math.ceil(cur * factor).toInt
        val nextAttempt = attempt + 1

        after(nextTry.milliseconds, as.scheduler, ec,
          () => envelopeSummary(envelopeId, nextTry, nextAttempt)
        )
      case _ =>
        Future.failed(new RuntimeException(s"[FileUploadConnector][retry] envelope[$envelopeId] summary failed at attempt: $attempt"))
    }
  }

  def envelopeSummary(envelopeId: String, nextTry: Int = firstRetryMilliseconds, attempt: Int = 1)
                     (implicit hc: HeaderCarrier): Future[Envelope] = {
    httpClientV2.get(url"$fileUploadUrl/file-upload/envelopes/$envelopeId").execute[HttpResponse].flatMap {
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
    response.header("Location").map(
      path =>
        path.split("/").reverse.head
    )
  }
}
