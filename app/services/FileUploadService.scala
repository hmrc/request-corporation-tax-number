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

package services

import javax.inject.Inject

import com.google.inject.Singleton
import connectors.FileUploadConnector
import model.Envelope
import model.domain.MimeContentType
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

@Singleton
class FileUploadService @Inject()(
                                 val fileUploadConnector: FileUploadConnector
                                 ) {

  def createEnvelope()(implicit hc: HeaderCarrier): Future[String] = {
    Logger.info(s"[FileUploadService][createEnvelope][creating envelope")
    fileUploadConnector.createEnvelope
  }

  def uploadFile(data: Array[Byte], envelopeId: String, fileName: String, contentType: MimeContentType)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    Logger.info(s"[FileUploadService][uploadFile][uploading file $envelopeId $fileName ${contentType.description}")
    fileUploadConnector.uploadFile(data, fileName, contentType, envelopeId, removeExtension(fileName))
  }

  def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] = {
    Logger.info(s"[FileUploadService][closeEnvelope][closing envelope $envelopeId")
    fileUploadConnector.closeEnvelope(envelopeId)
  }

  def envelopeSummary(envelopeId: String)(implicit hc: HeaderCarrier): Future[Envelope] = {
    fileUploadConnector.envelopeSummary(envelopeId)
  }

  private def removeExtension(fileName: String): String = fileName.split("\\.").head
}