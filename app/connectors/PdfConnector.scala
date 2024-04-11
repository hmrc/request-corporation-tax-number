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

package connectors

import config.MicroserviceAppConfig
import play.api.Logging
import play.api.http.Status
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HttpException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class PdfConnector @Inject()(val appConfig : MicroserviceAppConfig,
                             val wsClient: WSClient,
                             implicit val ec: ExecutionContext
                            ) extends Logging {

  private val basicUrl: String = s"${appConfig.pdfServiceUrl}/pdf-generator-service/generate"

  def generatePdf(html: String)(implicit hc: HeaderCarrier): Future[Array[Byte]] = {
    val headers: Seq[(String, String)] = hc.extraHeaders
    val body: Map[String,Seq[String]] = Map("html" -> Seq(html))
    wsClient
      .url(basicUrl)
      .addHttpHeaders(headers: _*)
      .post(body)
      .map { response =>
      response.status match {
        case Status.OK =>
          logger.info(s"[PdfConnector][generatePdf] PDF Generator Service successfully generated PDF")
          response.bodyAsBytes.toArray
        case _ =>
          logger.warn(s"[PdfConnector][generatePdf] A server error was received from PDF Generator Service. " +
            s"Status: ${response.status}. Body: ${response.body}.")
          throw new HttpException(response.body, response.status)
      }
    }
  }
}
