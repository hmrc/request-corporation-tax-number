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

import config.MicroserviceAppConfig
import model.domain.SubmissionResponse
import model.templates.CTUTRMetadata
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, FORBIDDEN, UNAUTHORIZED}
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsConnector @Inject()(httpClient: HttpClientV2)(implicit appConfig: MicroserviceAppConfig) extends Logging {

  private val internalAuthToken: String = appConfig.authToken

  private val dateOfReceipt: String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
    LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
  )

  private def dataParts(ctutrMetadata: CTUTRMetadata): Seq[MultipartFormData.DataPart] = Seq(
    MultipartFormData.DataPart("submissionReference", ctutrMetadata.submissionReference),
    MultipartFormData.DataPart("callbackUrl", appConfig.dmsSubmissionCallbackUrl),
    MultipartFormData.DataPart("metadata.store", ctutrMetadata.store),
    MultipartFormData.DataPart("metadata.source", ctutrMetadata.source),
    MultipartFormData.DataPart("metadata.timeOfReceipt", dateOfReceipt),
    MultipartFormData.DataPart("metadata.formId", ctutrMetadata.formId),
    MultipartFormData.DataPart("metadata.customerId", ctutrMetadata.customerId),
    MultipartFormData.DataPart("metadata.casKey", ctutrMetadata.casKey),
    MultipartFormData.DataPart("metadata.classificationType", ctutrMetadata.classificationType),
    MultipartFormData.DataPart("metadata.businessArea", ctutrMetadata.businessArea)
  )

  private def mapMultipartFormData(submissionReference: String,
                                   pdf: ByteString,
                                   pdfFileName: String,
                                   robotXml: ByteString,
                                   robotXmlFileName: String)(implicit hc: HeaderCarrier): Seq[MultipartFormData.FilePart[Source[ByteString, NotUsed]]] =
    Seq(
      MultipartFormData.FilePart(
        key = "form",
        filename = pdfFileName,
        contentType = Some("application/octet-stream"),
        ref = Source.single(pdf)
      ),
      MultipartFormData.FilePart(
        key = "attachment",
        filename = robotXmlFileName,
        contentType = Some("application/octet-stream"),
        ref = Source.single(robotXml)
      )
    )

  def postFileData(ctutrMetadata: CTUTRMetadata,
                   pdf: ByteString,
                   pdfFileName: String,
                   robotXml: ByteString,
                   robotXmlFileName: String)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {
    val dataPart: Seq[MultipartFormData.DataPart] = dataParts(ctutrMetadata)
    val multiFormData: Seq[MultipartFormData.FilePart[Source[ByteString, NotUsed]]] =
      mapMultipartFormData(ctutrMetadata.submissionReference, pdf, pdfFileName, robotXml, robotXmlFileName)
    val body: Source[MultipartFormData.Part[Source[ByteString, _]], NotUsed] = Source(
      dataPart ++ multiFormData
    )
    executeRequest(body)
      .flatMap( response =>
        response.status match {
          case ACCEPTED =>
            Future.successful(SubmissionResponse(ctutrMetadata.submissionReference, pdfFileName))
          case BAD_REQUEST =>
            logger.error(s"[SubmissionService][submit]: dms connector returned bad request response ${response.body}")
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
          case UNAUTHORIZED =>
            logger.error(s"[SubmissionService][submit]: dms connector returned unauthorized")
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
          case FORBIDDEN =>
            logger.error(s"[SubmissionService][submit]: dms connector returned forbidden")
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
          case _ =>
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
        }
      )
  }

  private def executeRequest(body: Source[MultipartFormData.Part[Source[ByteString, _]], NotUsed])
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    httpClient
      .post(url"${appConfig.dmsSubmissionBaseUrl}/dms-submission/submit")
      .setHeader("Authorization" -> internalAuthToken)
      .withBody(body)
      .execute[HttpResponse]
  }
}
