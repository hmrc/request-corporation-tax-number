package services

import com.google.inject.{Inject, Singleton}
import connectors.PdfConnector

import scala.concurrent.Future

@Singleton
class PdfService @Inject()(
                          val pdfConnector: PdfConnector
                          ){

  def generatePdf(html: String): Future[Array[Byte]] = pdfConnector.generatePdf(html)

}