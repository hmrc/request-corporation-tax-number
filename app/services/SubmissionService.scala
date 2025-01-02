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

import config.MicroserviceAppConfig
import connectors.DmsConnector
import model.domain.{MimeContentType, SubmissionResponse}
import model.templates.{CTUTRMetadata, SubmissionViewModel}
import model.{Envelope, Submission}
import org.apache.pekko.NotUsed
import play.api.Logging
import play.twirl.api.HtmlFormat
import templates.html.CTUTRScheme
import templates.xml.{pdfSubmissionMetadata, robotXml}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

trait EnvelopeStatus
case object Closed extends EnvelopeStatus
case object Open extends EnvelopeStatus

@Singleton
class SubmissionService @Inject()(
                                   dmsConnector: DmsConnector,
                                   pdfService: PdfGeneratorService,
                                   appConfig : MicroserviceAppConfig,
                                   implicit val ec: ExecutionContext
                                 ) extends Logging {

  // TODO: Is there a better way to return the fileName, can we parse it in>
  def submitPdfToDms(submission: Submission, fileName: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    dmsConnector.postFileData(
      createPdf(submission),
      fileName,
      MimeContentType.ApplicationPdf
    )

  def createMetadata(metadata: CTUTRMetadata): Array[Byte] = {
    pdfSubmissionMetadata(metadata).toString().getBytes
  }

  def createRobotXml(submission: Submission, metadata: CTUTRMetadata): Array[Byte] = {
    val viewModel = SubmissionViewModel.apply(submission)
    robotXml(metadata, viewModel).toString().getBytes
  }

  def createPdf(submission: Submission)(implicit hc: HeaderCarrier): Source[ByteString, NotUsed] = {
    val viewModel: SubmissionViewModel = SubmissionViewModel.apply(submission)
    val pdfTemplate: HtmlFormat.Appendable = CTUTRScheme(viewModel)
    val xlsTransformer: String = scala.io.Source.fromResource("CTUTRScheme.xml").mkString
    Source.future(pdfService.render(pdfTemplate, xlsTransformer).map(ByteString(_)))
  }

}
