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
import play.api.mvc.MultipartFormData._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsConnector @Inject()(httpClient: HttpClientV2)(implicit appConfig: MicroserviceAppConfig) extends Logging {

  private val internalAuthToken: String = appConfig.authToken

  def constructMultipartFormData(ctutrMetadata: CTUTRMetadata,
                                 pdf: ByteString,
                                 pdfFileName: String,
                                 robotXml: ByteString,
                                 robotXmlFileName: String,
                                 dateOfReceipt: String): Source[Part[Source[ByteString, _]], NotUsed] =
    Source(
      Seq(
        DataPart("submissionReference", ctutrMetadata.submissionReference),
        DataPart("callbackUrl", appConfig.dmsSubmissionCallbackUrl),
        DataPart("metadata.store", ctutrMetadata.store),
        DataPart("metadata.source", ctutrMetadata.source),
        DataPart("metadata.timeOfReceipt", dateOfReceipt),
        DataPart("metadata.formId", ctutrMetadata.formId),
        DataPart("metadata.customerId", ctutrMetadata.customerId),
        DataPart("metadata.casKey", ctutrMetadata.casKey),
        DataPart("metadata.classificationType", ctutrMetadata.classificationType),
        DataPart("metadata.businessArea", ctutrMetadata.businessArea),
        FilePart(
          key = "form",
          filename = pdfFileName,
          contentType = Some("application/octet-stream"),
          ref = Source.single(pdf)
        ),
        FilePart(
          key = "attachment",
          filename = robotXmlFileName,
          contentType = Some("application/octet-stream"),
          ref = Source.single(robotXml)
        )
      )
    )

  def postFileData(ctutrMetadata: CTUTRMetadata,
                   pdf: ByteString,
                   pdfFileName: String,
                   robotXml: ByteString,
                   robotXmlFileName: String,
                   dateOfReceipt: String)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] =
    httpClient
      .post(url"${appConfig.dmsSubmissionBaseUrl}/dms-submission/submit")
      .setHeader("Authorization" -> internalAuthToken)
      .withBody(
        constructMultipartFormData(ctutrMetadata, pdf, pdfFileName, robotXml, robotXmlFileName, dateOfReceipt)
      )
      .execute[HttpResponse]
      .flatMap{ response: HttpResponse =>
        response.status match {
          case ACCEPTED =>
            Future.successful(SubmissionResponse(ctutrMetadata.submissionReference, pdfFileName))
          case BAD_REQUEST =>
            logger.error(s"[SubmissionService][submit]: dms connector returned bad request response, body: ${response.body}")
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
          case UNAUTHORIZED =>
            logger.error(s"[SubmissionService][submit]: dms connector returned unauthorized, body: ${response.body}")
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
          case FORBIDDEN =>
            logger.error(s"[SubmissionService][submit]: dms connector returned forbidden, body: ${response.body}")
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
          case _ =>
            logger.error(s"[SubmissionService][submit]: dms connector returned an error, body: ${response.body}")
            Future.failed(new RuntimeException(s"Failed with status [${response.status}]"))
        }
      }
}
