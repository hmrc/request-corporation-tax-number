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
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import config.MicroserviceAppConfig
import model.Envelope
import model.domain.MimeContentType
import play.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class FileUploadConnector @Inject()(
                                     appConfig : MicroserviceAppConfig,
                                     val httpClient : HttpClient,
                                     val wsClient : WSClient,
                                     val metrics : Metrics
                                   ){

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  def callbackUrl: String = appConfig.fileUploadCallbackUrl
  def fileUploadUrl: String = appConfig.fileUploadUrl
  def fileUploadFrontEndUrl: String = appConfig.fileUploadFrontendUrl

  def routingRequest(envelopeId: String): JsValue = Json.obj(
    "envelopeId" -> envelopeId,
    "application" -> "CTUTR",
    "destination" ->"DMS")

  def createEnvelopeBody: JsValue = Json.obj("callbackUrl" -> callbackUrl)

  def createEnvelope(implicit hc: HeaderCarrier): Future[String] = {

    httpClient.POST[JsValue, HttpResponse](s"$fileUploadUrl/file-upload/envelopes", createEnvelopeBody).map { response =>

      if (response.status == CREATED) {

        envelopeId(response)
          .getOrElse {
            Logger.warn("[FileUploadConnector][createEnvelope] No envelope id returned by file upload service")
            throw new RuntimeException("No envelope id returned by file upload service")
          }
      } else {
        Logger.warn(s"[FileUploadConnector][createEnvelope] - failed to create envelope with status [${response.status}]")
        throw new RuntimeException("File upload envelope creation failed")
      }
    }.recover {
      case _: Exception =>
        Logger.warn("[FileUploadConnector][createEnvelope] - call to create envelope failed")
        throw new RuntimeException("File upload envelope creation failed")
    }
  }

  def uploadFile(byteArray: Array[Byte], fileName: String, contentType: MimeContentType, envelopeId: String, fileId: String)
                (implicit hc: HeaderCarrier): Future[HttpResponse] = {



    val multipartFormData = Source(FilePart("attachment", fileName, Some(contentType.description),
      Source(ByteString(byteArray) :: Nil)) :: DataPart("", "") :: Nil)

    wsClient.url(s"$fileUploadFrontEndUrl/file-upload/upload/envelopes/$envelopeId/files/$fileId")
      .withHeaders(hc.copy(otherHeaders = Seq("CSRF-token" -> "nocheck")).headers: _*).post(multipartFormData).map { response =>

      if (response.status == OK) {
        HttpResponse(response.status)
      } else {
        Logger.warn(s"[FileUploadConnector][uploadFile] - failed to upload file with status [${response.status}]")
        throw new RuntimeException("File upload failed")
      }
    }.recover {
      case _: Exception =>
        Logger.warn("[FileUploadConnector][uploadFile] - call to upload file failed")
        throw new RuntimeException("File upload failed")
    }
  }

  def closeEnvelope(envId: String)(implicit hc: HeaderCarrier): Future[String] = {
    httpClient.POST[JsValue, HttpResponse](s"$fileUploadUrl/file-routing/requests", routingRequest(envId)).map { response =>
      if (response.status == CREATED) {
        envelopeId(response)
          .getOrElse {
            Logger.warn("[FileUploadConnector][closeEnvelope] No envelope id returned by file upload service")
            throw new RuntimeException("No envelope id returned by file upload service")
          }
      } else {
        Logger.warn(s"[FileUploadConnector][closeEnvelope] failed to close envelope with status [${response.status}]")
        throw new RuntimeException("File upload envelope routing request failed")
      }
    }.recover{
      case e: Throwable =>
        if(e.getMessage.contains("Routing request already received for envelope")){
          Logger.warn("[FileUploadConnector][closeEnvelope] call to close envelope that has already been closed")
          ""
        }else{
          Logger.error("[FileUploadConnector][closeEnvelope] call to close envelope failed", e)
          throw new RuntimeException("File upload call to close envelope failed")
        }
    }
  }

  def envelopeSummary(envelopeId: String)(implicit hc: HeaderCarrier): Future[Envelope] = {
    Logger.info("[FileUploadConnector][envelopeSummary] request envelope summary from file upload")

    val envelope = httpClient.GET[Envelope](s"$fileUploadUrl/file-upload/envelopes/$envelopeId")

    envelope.onFailure {
      case e: Throwable =>
        Logger.error("[FileUploadConnector][envelopeSummary] failed to get envelope summary from file upload", e)
    }

    envelope
  }

  private def envelopeId(response: HttpResponse): Option[String] = {
    response.header("Location").map(path =>
      path.split("/")
        .reverse
        .head)
  }
}