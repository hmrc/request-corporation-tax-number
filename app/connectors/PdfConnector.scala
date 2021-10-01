/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors

import config.MicroserviceAppConfig
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.http.HeaderCarrier
import utils.CorrelationIdHelper
import uk.gov.hmrc.http.HttpResponse

@Singleton
class PdfConnector @Inject()(val appConfig : MicroserviceAppConfig,
                             val httpClient: DefaultHttpClient,
                             implicit val ec: ExecutionContext
                            ) extends Logging with CorrelationIdHelper {

  private val basicUrl: String = s"${appConfig.pdfServiceUrl}/pdf-generator-service/generate"

  def generatePdf(html: String)(implicit hc: HeaderCarrier): Future[Array[Byte]] = {
    val body: Map[String,Seq[String]] = Map("html" -> Seq(html))
    httpClient.POST[Map[String,Seq[String]], HttpResponse](basicUrl, body).map { response => 
      response.status match {
        case Status.OK =>
          logger.info(s"[PdfConnector][generatePdf] [Generated PDF]")
          response.body.getBytes()
        case _ =>
          logger.warn(s"[PdfConnector][generatePdf][A Server error was received from PDF generator service]")
          throw new HttpException(response.body, response.status)
      }
    }
  }
}
