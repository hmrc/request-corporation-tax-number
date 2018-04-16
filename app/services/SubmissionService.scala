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
import model.Submission
import model.domain.{MimeContentType, SubmissionResponse}
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import org.joda.time.LocalDate
import play.api.Logger
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
                                appConfig : MicroserviceAppConfig
                                ){

  import scala.concurrent.ExecutionContext.Implicits.global

  private val FileUploadSuccessStatus = "AVAILABLE"
  private val FileUploadErrorStatus = "ERROR"

  private val FileUploadSuccessAudit = "FileUploadSuccess"
  private val FileUploadFailureAudit = "FileUploadFailure"

  protected def submissionFileName(envelopeId: String) = s"$envelopeId-SubmissionCTUTR-${LocalDate.now().toString("YYYYMMdd")}-iform.pdf"
  protected def submissionMetaDataName(envelopeId: String) = s"$envelopeId-SubmissionCTUTR-${LocalDate.now().toString("YYYYMMdd")}-metadata.xml"
  protected def submissionRobotName(envelopeId: String) = s"$envelopeId-SubmissionCTUTR-${LocalDate.now().toString("YYYYMMdd")}-robot.xml"

  def submit(submission: Submission)(implicit hc : HeaderCarrier) : Future[SubmissionResponse] = {
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

            fileUploadService.uploadFile(pdf, envelopeId, filename, MimeContentType.ApplicationPdf)
            fileUploadService.uploadFile(submissionMetadata, envelopeId, submissionMetaDataName(envelopeId), MimeContentType.ApplicationXml)
            fileUploadService.uploadFile(robotSubmission, envelopeId, submissionRobotName(envelopeId), MimeContentType.ApplicationXml)

            SubmissionResponse(envelopeId, filename)
        }
    }
  }

  def callback(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] = {
      fileUploadService.envelopeSummary(envelopeId).flatMap {
        envelope =>
          if (envelope.status == "OPEN") {
            if (envelope.files.forall(file => file.status == "AVAILABLE") && envelope.files.length == 3) {
              fileUploadService.closeEnvelope(envelopeId)
            } else {
              Logger.info("[SubmissionService][callback] incomplete wait for files")
              Future.successful(envelopeId)
            }
          } else {
            Logger.error("[SubmissionService][callback] envelope is not open")
            Future.successful(envelopeId)
          }
      }
  }
}
