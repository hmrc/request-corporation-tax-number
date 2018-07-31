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
import model.{Envelope, Submission}
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
                                ) {

  import scala.concurrent.ExecutionContext.Implicits.global

  protected def fileName(envelopeId: String, fileType: String) = s"$envelopeId-SubmissionCTUTR-${LocalDate.now().toString("YYYYMMdd")}-$fileType"

  def submit(submission: Submission)(implicit hc: HeaderCarrier): Future[SubmissionResponse] = {

    val handleUpload: Future[SubmissionResponse] = for {
      pdf: Array[Byte] <- createPdf(submission)
      envelopeId: String <- fileUploadService.createEnvelope()
      envelope: Envelope <- fileUploadService.envelopeSummary(envelopeId)
    } yield {
      Logger.info(s"[SubmissionService][submit] submission created $envelopeId")

      envelope.status match {
        case "OPEN" =>
          fileUploadService.uploadFile(
            pdf,
            envelopeId,
            fileName(envelopeId, "iform.pdf"),
            MimeContentType.ApplicationPdf
          )

          fileUploadService.uploadFile(
            createMetadata(submission),
            envelopeId,
            fileName(envelopeId, "metadata.xml"),
            MimeContentType.ApplicationXml
          )

          fileUploadService.uploadFile(
            createRobotXml(submission),
            envelopeId,
            fileName(envelopeId, "robotic.xml"),
            MimeContentType.ApplicationXml
          )
        case _ =>
          Logger.error(s"[SubmissionService][submit] Envelope status not OPEN for envelopeId: $envelopeId")
          Future.failed(throw new RuntimeException())
      }

      SubmissionResponse(envelopeId, fileName(envelopeId, "iform.pdf"))
    }

    handleUpload.recoverWith {
      case exception =>
        Future.failed(new RuntimeException("Submit Failed", exception))
    }
  }

  def createMetadata(submission: Submission): Array[Byte] = {
    val metadata = CTUTRMetadata(appConfig, submission.companyDetails.companyReferenceNumber)
    pdfSubmissionMetadata(metadata).toString().getBytes
  }

  def createRobotXml(submission: Submission): Array[Byte] = {
    val viewModel = SubmissionViewModel.apply(submission)
    val metadata = CTUTRMetadata(appConfig, submission.companyDetails.companyReferenceNumber)
    robotXml(metadata, viewModel).toString().getBytes
  }

  def createPdf(submission: Submission): Future[Array[Byte]] = {
    val viewModel = SubmissionViewModel.apply(submission)
    val pdfTemplate = CTUTRScheme(viewModel).toString
    pdfService.generatePdf(pdfTemplate)
  }

  def callback(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] = {
    fileUploadService.envelopeSummary(envelopeId).flatMap {
      envelope =>
        envelope.status match {
          case "OPEN" =>
            envelope.files match {
              case Some(files) if files.count(file => file.status == "AVAILABLE") == 3 =>
                fileUploadService.closeEnvelope(envelopeId)
              case _=>
                Logger.info("[SubmissionService][callback] incomplete wait for files")
                Future.successful(envelopeId)
            }
          case _ =>
            Logger.error(s"[SubmissionService][callback] envelope: $envelopeId not open instead status: ${envelope.status}")
            Future.successful(envelopeId)
        }
    }
  }

}
