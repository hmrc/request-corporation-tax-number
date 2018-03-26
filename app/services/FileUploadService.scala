package services

import javax.inject.Inject

import com.google.inject.Singleton
import connectors.FileUploadConnector
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

  private def removeExtension(fileName: String): String = fileName.split("\\.").head

}