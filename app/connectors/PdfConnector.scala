/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kenshoo.play.metrics.Metrics
import config.MicroserviceAppConfig
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PdfConnector @Inject()(val appConfig : MicroserviceAppConfig,
                             val httpClient: HttpClient,
                             metrics : Metrics,
                             implicit val ec: ExecutionContext
                            ) {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val basicUrl: String = s"${appConfig.pdfServiceUrl}/pdf-generator-service/generate"

  def generatePdf(html: String)(implicit hc: HeaderCarrier): Future[Array[Byte]] = {
    httpClient.doFormPost(basicUrl, body = Map("html" -> Seq(html))).map { response =>
      response.status match {
        case Status.OK =>
          Logger.info(s"[PdfConnector][generatePdf] [Generated PDF]")
          response.body.getBytes()
        case _ =>
          Logger.warn(s"[PdfConnector][generatePdf][A Server error was received from PDF generator service]")
          throw new HttpException(response.body, response.status)
      }
    }
  }
}
