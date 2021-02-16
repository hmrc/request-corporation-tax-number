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
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HttpException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PdfConnector @Inject()(val appConfig : MicroserviceAppConfig,
                             val wsClient: WSClient,
                             implicit val ec: ExecutionContext
                            ) extends Logging {

  private val basicUrl: String = s"${appConfig.pdfServiceUrl}/pdf-generator-service/generate"

  def generatePdf(html: String): Future[Array[Byte]] = {
    wsClient.url(basicUrl).post(body = Map("html" -> Seq(html))).map { response =>
      response.status match {
        case Status.OK =>
          logger.info(s"[PdfConnector][generatePdf] [Generated PDF]")
          response.bodyAsBytes.toArray
        case _ =>
          logger.warn(s"[PdfConnector][generatePdf][A Server error was received from PDF generator service]")
          throw new HttpException(response.body, response.status)
      }
    }
  }
}
