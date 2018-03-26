package services

import javax.inject.Inject

import com.google.inject.Singleton
import config.MicroserviceAppConfig
import model.domain.{EnrolmentResponse, MimeContentType}
import model.templates.{EnrolmentViewModel, SCC1Metadata}
import model.{Enrolment, FileUploadCallback, IFormDetails}
import org.joda.time.LocalDate
import play.api.Logger
import repositories.EnrolmentRepository
import templates.html.EnrolSocialCareComplianceScheme
import templates.xml.pdfSubmissionMetadata
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait EnvelopeStatus
case object Closed extends EnvelopeStatus
case object Open extends EnvelopeStatus

@Singleton
class EnrolmentService @Inject()(
                                val fileUploadService: FileUploadService,
                                val pdfService: PdfService,
                                val enrolmentRepository: EnrolmentRepository,
                                appConfig : MicroserviceAppConfig
                                ){

  import scala.concurrent.ExecutionContext.Implicits.global

  private val FileUploadSuccessStatus = "AVAILABLE"
  private val FileUploadErrorStatus = "ERROR"

  private val FileUploadSuccessAudit = "FileUploadSuccess"
  private val FileUploadFailureAudit = "FileUploadFailure"

  protected def enrolmentFileName(envelopeId: String) = s"$envelopeId-EnrolSocialCareCompliance-${LocalDate.now().toString("YYYYMMdd")}-iform.pdf"
  private def enrolmentMetaDataName(envelopeId: String) = s"$envelopeId-EnrolSocialCareCompliance-${LocalDate.now().toString("YYYYMMdd")}-metadata.xml"

  def enrol(enrolment : Enrolment)(implicit hc : HeaderCarrier) : Future[EnrolmentResponse] = {
    val viewModel = EnrolmentViewModel.apply(enrolment)
    val pdfTemplate = EnrolSocialCareComplianceScheme(viewModel).toString

    pdfService.generatePdf(pdfTemplate) flatMap {
      pdf =>
        Logger.info(s"[EnrolmentService][enrol][PDF generated], attempting to create envelope")
        fileUploadService.createEnvelope() map {
          envelopeId =>
            Logger.info(s"[EnrolmentService][enrol] enrolment created $envelopeId")

            val filename = enrolmentFileName(envelopeId)
            val metadata = SCC1Metadata(appConfig)

            val enrolmentMetadata = pdfSubmissionMetadata(metadata).toString().getBytes
            enrolmentRepository.updateIFormDetails(envelopeId, IFormDetails(pdfUploaded = false, metadataUploaded = false))

            fileUploadService.uploadFile(pdf, envelopeId, filename, MimeContentType.ApplicationPdf)
            fileUploadService.uploadFile(enrolmentMetadata, envelopeId, enrolmentMetaDataName(envelopeId), MimeContentType.ApplicationXml)

            EnrolmentResponse(envelopeId, filename)
        }
    }
  }

  def fileUploadCallback(details: FileUploadCallback)(implicit hc: HeaderCarrier): Future[EnvelopeStatus] = {
    if (details.status == FileUploadSuccessStatus) {
      Logger.info(s"[EnrolmentService][fileUploadCallback] [FileUploadSuccess]")
      callback(details)
    } else if(details.status == FileUploadErrorStatus) {
      Logger.info(s"[EnrolmentService][fileUploadCallback] [FileUploadError]")
      Future.successful(Open)
    } else {
      Logger.info(s"[EnrolmentService][fileUploadCallback] [Status undetermined]")
      Future.successful(Open)
    }
  }

  private def callback(details: FileUploadCallback)(implicit hc: HeaderCarrier) = {
    enrolmentRepository.iFormDetails(details.envelopeId) map {
      case Some(iFormDetails) =>
        if (!iFormDetails.pdfUploaded && !iFormDetails.metadataUploaded) {
          enrolmentRepository.updateIFormDetails(details.envelopeId, createIFormDetails(details))
          Logger.info(s"[EnrolmentService][callback] Creating new iForm mongo record ${details.fileId} ${details.status}")
          Open
        } else if (iFormDetails.metadataUploaded && details.fileId.contains("metadata")) {
          Logger.warn(s"[EnrolmentService][callback] Received callback multiple times for Metadata File ${details.fileId}")
          Open
        } else if (iFormDetails.pdfUploaded && details.fileId.contains("iform")) {
          Logger.warn(s"[EnrolmentService][callback] Received callback multiple times for PDF File ${details.fileId}")
          Open
        } else {
          Logger.info(s"[EnrolmentService][callback][Closing envelope] ${details.fileId}")
          fileUploadService.closeEnvelope(details.envelopeId)
          enrolmentRepository.removeIFormDetails(details.envelopeId)
          Closed
        }
      case None =>
        throw new RuntimeException(s"Data not found for envelope-id ${details.envelopeId}")
    }
  }

  private def createIFormDetails(details: FileUploadCallback): IFormDetails = {
    if(details.fileId.contains("metadata")){
      Logger.info(s"[EnrolmentService][createIFormDetails][meta data uploaded for enrolment]")
      IFormDetails(pdfUploaded = false, metadataUploaded = true)
    } else {
      Logger.info(s"[EnrolmentService][createIFormDetails][pdf uploaded for enrolment]")
      IFormDetails(pdfUploaded = true, metadataUploaded = false)
    }
  }

}