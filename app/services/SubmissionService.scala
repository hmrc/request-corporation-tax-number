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

package services

import model.domain.{MimeContentType, SubmissionResponse}
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import model.{Envelope, Submission}
import play.api.Logging
import play.twirl.api.HtmlFormat
import templates.html.CTUTRScheme
import templates.xml.{pdfSubmissionMetadata, robotXml}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

trait EnvelopeStatus
case object Closed extends EnvelopeStatus
case object Open extends EnvelopeStatus

@Singleton
class SubmissionService @Inject()(
                                   val fileUploadService: FileUploadService,
                                   pdfService: PdfGeneratorService,
                                   implicit val ec: ExecutionContext
                                 ) extends Logging {

  protected def fileName(envelopeId: String, fileType: String) =
    s"$envelopeId-SubmissionCTUTR-${LocalDate.now().format(DateTimeFormatter.ofPattern("YYYYMMdd"))}-$fileType"

  def submit(submission: Submission, metadata: CTUTRMetadata)(implicit hc: HeaderCarrier): Future[SubmissionResponse] = {

    val handleUpload: Future[SubmissionResponse] = for {
      pdf: Array[Byte] <- createPdf(submission, metadata)
      envelopeId: String <- fileUploadService.createEnvelope()
      envelope: Envelope <- fileUploadService.envelopeSummary(envelopeId)
    } yield {
      logger.info(s"[SubmissionService][submit] submission created $envelopeId")

      envelope.status match {
        case "OPEN" =>
          fileUploadService.uploadFile(
            pdf,
            envelopeId,
            fileName(envelopeId, "iform.pdf"),
            MimeContentType.ApplicationPdf
          )

          fileUploadService.uploadFile(
            createMetadata(metadata),
            envelopeId,
            fileName(envelopeId, "metadata.xml"),
            MimeContentType.ApplicationXml
          )

          fileUploadService.uploadFile(
            createRobotXml(submission, metadata),
            envelopeId,
            fileName(envelopeId, "robotic.xml"),
            MimeContentType.ApplicationXml
          )
        case _ =>
          logger.error(s"[SubmissionService][submit] Envelope status not OPEN for envelopeId: $envelopeId")
          Future.failed(throw new RuntimeException())
      }

      SubmissionResponse(envelopeId, fileName(envelopeId, "iform.pdf"))
    }

    handleUpload.recoverWith {
      case exception =>
        Future.failed(new RuntimeException("Submit Failed", exception))
    }
  }

  def createMetadata(metadata: CTUTRMetadata): Array[Byte] = {
    pdfSubmissionMetadata(metadata).toString().getBytes
  }

  def createRobotXml(submission: Submission, metadata: CTUTRMetadata): Array[Byte] = {
    val viewModel = SubmissionViewModel(submission, metadata)
    robotXml(metadata, viewModel).toString().getBytes
  }

  def createPdf(submission: Submission, metadata: CTUTRMetadata): Future[Array[Byte]] = {
    val viewModel: SubmissionViewModel = SubmissionViewModel(submission, metadata)
    val pdfTemplate: HtmlFormat.Appendable = CTUTRScheme(viewModel)
    val xlsTransformer: String = Source.fromResource("CTUTRScheme.xml").mkString
    pdfService.render(pdfTemplate, xlsTransformer)
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
                logger.info("[SubmissionService][callback] incomplete wait for files")
                Future.successful(envelopeId)
            }
          case _ =>
            logger.error(s"[SubmissionService][callback] envelope: $envelopeId not open instead status: ${envelope.status}")
            Future.successful(envelopeId)
        }
    }
  }

}
