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
import config.MicroserviceAppConfig
import model.domain.{MimeContentType, SubmissionResponse}
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import model.{FileUploadCallback, Submission, SubmissionDetails}
import org.joda.time.LocalDate
import play.api.Logger
import repositories.SubmissionRepository
import templates.html.CTUTRScheme
import templates.xml.{pdfSubmissionMetadata, robotXml}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait EnvelopeStatus
case object Closed extends EnvelopeStatus
case object Open extends EnvelopeStatus

@Singleton
class SubmissionService @Inject()(
                                val fileUploadService: FileUploadService,
                                val pdfService: PdfService,
                                val submissionRepository: SubmissionRepository,
                                appConfig : MicroserviceAppConfig
                                ){

  import scala.concurrent.ExecutionContext.Implicits.global

  private val FileUploadSuccessStatus = "AVAILABLE"
  private val FileUploadErrorStatus = "ERROR"

  private val FileUploadSuccessAudit = "FileUploadSuccess"
  private val FileUploadFailureAudit = "FileUploadFailure"

  protected def submissionFileName(envelopeId: String) = s"$envelopeId-SubmissionCTUTR-${LocalDate.now().toString("YYYYMMdd")}-iform.pdf"
  private def submissionMetaDataName(envelopeId: String) = s"$envelopeId-SubmissionCTUTR-${LocalDate.now().toString("YYYYMMdd")}-metadata.xml"
  private def submissionRobotName(envelopeId: String) = s"$envelopeId-SubmissionCTUTR-${LocalDate.now().toString("YYYYMMdd")}-robot.xml"

  def submit(submission : Submission)(implicit hc : HeaderCarrier) : Future[SubmissionResponse] = {
    val viewModel = SubmissionViewModel.apply(submission)
    val pdfTemplate = CTUTRScheme(viewModel).toString

    pdfService.generatePdf(pdfTemplate) flatMap {
      pdf =>
        Logger.info(s"[SubmissionService][submit][PDF generated], attempting to create envelope")
        fileUploadService.createEnvelope() map {
          envelopeId =>
            Logger.info(s"[SubmissionService][submit] submission created $envelopeId")

            val filename = submissionFileName(envelopeId)
            val metadata = CTUTRMetadata(appConfig)

            val submissionMetadata = pdfSubmissionMetadata(metadata).toString().getBytes
            val robotSubmission = robotXml(metadata,viewModel).toString().getBytes
            submissionRepository.updateSubmissionDetails(envelopeId, SubmissionDetails(pdfUploaded = false, metadataUploaded = false, robotXmlUploaded = false))

            fileUploadService.uploadFile(pdf, envelopeId, filename, MimeContentType.ApplicationPdf)
            fileUploadService.uploadFile(submissionMetadata, envelopeId, submissionMetaDataName(envelopeId), MimeContentType.ApplicationXml)
            fileUploadService.uploadFile(robotSubmission, envelopeId, submissionRobotName(envelopeId), MimeContentType.ApplicationXml)

            SubmissionResponse(envelopeId, filename)
        }
    }
  }

  def fileUploadCallback(details: FileUploadCallback)(implicit hc: HeaderCarrier): Future[EnvelopeStatus] = {
    if (details.status == FileUploadSuccessStatus) {
      Logger.info(s"[SubmissionService][fileUploadCallback] [FileUploadSuccess]")
      callback(details)
    } else if(details.status == FileUploadErrorStatus) {
      Logger.info(s"[SubmissionService][fileUploadCallback] [FileUploadError]")
      Future.successful(Open)
    } else {
      Logger.info(s"[SubmissionService][fileUploadCallback] [Status undetermined]")
      Future.successful(Open)
    }
  }

  private def callback(details: FileUploadCallback)(implicit hc: HeaderCarrier): Future[EnvelopeStatus] = {
    submissionRepository.submissionDetails(details.envelopeId) flatMap {
      case Some(submissionDetails) =>
        if (!submissionDetails.pdfUploaded && !submissionDetails.metadataUploaded && !submissionDetails.robotXmlUploaded) {
          submissionRepository.updateSubmissionDetails(details.envelopeId, createSubmissionDetails(details))
          Logger.info(s"[SubmissionService][callback] Creating new iForm mongo record ${details.fileId} ${details.status}")
          Future.successful(Open)
        } else if (submissionDetails.metadataUploaded && details.fileId.contains("metadata")) {
          Logger.warn(s"[SubmissionService][callback] Received callback multiple times for Metadata File ${details.fileId}")
          Future.successful(Open)
        } else if (submissionDetails.pdfUploaded && details.fileId.contains("iform")) {
          Logger.warn(s"[SubmissionService][callback] Received callback multiple times for PDF File ${details.fileId}")
          Future.successful(Open)
        } else if (submissionDetails.robotXmlUploaded && details.fileId.contains("robot")) {
          Logger.warn(s"[SubmissionService][callback] Received callback multiple times for Robot File ${details.fileId}")
          Future.successful(Open)
        } else {
          Logger.info(s"[SubmissionService][callback][Closing envelope] ${details.fileId}")
          fileUploadService.closeEnvelope(details.envelopeId)
          submissionRepository.removeSubmissionDetails(details.envelopeId)
          Future.successful(Closed)
        }
      case None =>
        val e = new RuntimeException(s"Data not found for envelope-id ${details.envelopeId}")
        Logger.error(s"[SubmissionService][callback] No envelop found for envelope-id ${details.envelopeId}", e)
        Future.failed(e)
    }
  }

  private def createSubmissionDetails(details: FileUploadCallback): SubmissionDetails = {
    if(details.fileId.contains("metadata")){
      Logger.info(s"[SubmissionService][createIFormDetails][meta data uploaded for submission]")
      SubmissionDetails(pdfUploaded = false, metadataUploaded = true, robotXmlUploaded = false)
    } else if (details.fileId.contains("iform")){
      Logger.info(s"[SubmissionService][createIFormDetails][pdf uploaded for submission]")
      SubmissionDetails(pdfUploaded = true, metadataUploaded = false, robotXmlUploaded = false)
    } else {
      Logger.info(s"[SubmissionService][createRobotXMLDetails][pdf uploaded for submission]")
      SubmissionDetails(pdfUploaded = false, metadataUploaded = false, robotXmlUploaded = true)
    }
  }

}