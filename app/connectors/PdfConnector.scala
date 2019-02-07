/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kenshoo.play.metrics.Metrics
import config.MicroserviceAppConfig
import play.api.Logger
import play.api.http.Status
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HttpException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PdfConnector @Inject()(
                            val appConfig : MicroserviceAppConfig,
                            val wsClient: WSClient,
                            metrics : Metrics
                            ) {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  def serviceUrl: String = appConfig.pdfServiceUrl

  private[connectors] def basicUrl = s"$serviceUrl/pdf-generator-service/generate"

  def generatePdf(html: String): Future[Array[Byte]] = {

    val result = wsClient.url(basicUrl).post(Map("html" -> Seq(html)))

    result.map { response =>
      response.status match {
        case Status.OK =>
          Logger.info(s"[PdfConnector][generatePdf] [Generated PDF]")
          response.bodyAsBytes.toArray
        case _ =>
          Logger.warn(s"[PdfConnector][generatePdf][A Server error was received from PDF generator service]")
          throw new HttpException(response.body, response.status)
      }
    }
  }
}
