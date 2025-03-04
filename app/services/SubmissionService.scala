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

package services

import connectors.DmsConnector
import model.domain.SubmissionResponse
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import model.Submission
import play.api.Logging
import play.twirl.api.HtmlFormat
import templates.html.CTUTRScheme
import templates.xml.robotXml
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import org.apache.pekko.util.ByteString

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Singleton
class SubmissionService @Inject()(
                                   dmsConnector: DmsConnector,
                                   pdfService: PdfGeneratorService
                                 )(implicit val ec: ExecutionContext) extends Logging {

  private def fileName(submissionReference: String, fileType: String): String =
    s"$submissionReference-${LocalDate.now().format(DateTimeFormatter.ofPattern("YYYYMMdd"))}-$fileType"

  private def dateOfReceipt: String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
    LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
  )

  def submit(ctutrMetadata: CTUTRMetadata, submission: Submission)(implicit hc: HeaderCarrier): Future[SubmissionResponse] = {
    val pdfFileName: String = fileName(ctutrMetadata.submissionReference, "iform.pdf")
    val robotXmlFileName: String = fileName(s"${ctutrMetadata.submissionReference}-SubmissionCTUTR", "robotic.xml")
    val pdfTemplate: HtmlFormat.Appendable = CTUTRScheme(SubmissionViewModel.apply(submission))
    val xlsTransformer: String = scala.io.Source.fromResource("CTUTRScheme.xml").mkString
    for {
      pdf: ByteString <- createPdf(pdfTemplate, xlsTransformer)
      robotXml: ByteString = createRobotXml(submission, ctutrMetadata)
      dmsResponse: SubmissionResponse <- dmsConnector.postFileData(
        ctutrMetadata,
        pdf,
        pdfFileName,
        robotXml,
        robotXmlFileName,
        dateOfReceipt
      )
    } yield dmsResponse
  }

  private def createRobotXml(submission: Submission, metadata: CTUTRMetadata): ByteString =
    ByteString(
      robotXml(metadata, SubmissionViewModel.apply(submission))
        .toString()
        .getBytes
    )

  private def createPdf(pdfTemplate: HtmlFormat.Appendable, xlsTransformer: String): Future[ByteString] =
    pdfService
      .render(pdfTemplate, xlsTransformer)
      .map(ByteString(_))
      .recoverWith {
        case e: Exception =>
          throw new RuntimeException(s"[SubmissionService][createPdf] Error creating PDF: ${e.getMessage}")
      }
}
