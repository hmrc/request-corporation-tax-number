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

import config.MicroserviceAppConfig
import model.domain.MimeContentType
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsConnector @Inject()(
                               httpClient: HttpClientV2,
                             )(implicit ec: ExecutionContext, appConfig: MicroserviceAppConfig)
                              extends Logging {

  private val internalAuthToken: String = appConfig.authToken

  val dateOfReceipt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
    LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
  )

  val dataParts: Seq[MultipartFormData.DataPart] = Seq(
    MultipartFormData.DataPart("callbackUrl", appConfig.fileUploadCallbackUrl),
    MultipartFormData.DataPart("metadata.store", "true"),
    MultipartFormData.DataPart("metadata.source", "ct-utr"),
    MultipartFormData.DataPart("metadata.timeOfReceipt", dateOfReceipt),
    MultipartFormData.DataPart("metadata.formId", "formId"),
    MultipartFormData.DataPart("metadata.customerId", "customerId"),
    MultipartFormData.DataPart("metadata.submissionMark", "submissionMark"),
    MultipartFormData.DataPart("metadata.casKey", "casKey"),
    MultipartFormData.DataPart("metadata.classificationType", "classificationType"),
    MultipartFormData.DataPart("metadata.businessArea", "businessArea")
  )

  def mapMultipartFormData(
                            fileData: Source[ByteString, _],
                            fileName: String,
                            contentType: MimeContentType
                          ): Seq[MultipartFormData.FilePart[Source[ByteString, _]]] =
    Seq(
      MultipartFormData.FilePart(
        key = "form",
        filename = fileName,
        contentType = Some(contentType.description),
        ref = fileData
      )
    )

  def postFileData(pdfSource: Source[ByteString, NotUsed], fileName: String, contentType: MimeContentType)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val multipartFormData: Seq[MultipartFormData.FilePart[Source[ByteString, _]]] =
      mapMultipartFormData(pdfSource, fileName, contentType)

    val body: Source[MultipartFormData.Part[Source[ByteString, _]], NotUsed] = Source(
      dataParts ++ multipartFormData
    )

    httpClient
      .post(url"${appConfig.dmsSubmissionBaseUrl}/dms-submission/submit")
      .setHeader("Authorization" -> internalAuthToken)
      .withBody(body)
      .execute[HttpResponse]
  }
}
