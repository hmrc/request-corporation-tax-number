/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.testOnly

import config.MicroserviceAppConfig
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadEnvelopeConnector @Inject()(appConfig: MicroserviceAppConfig,
                                          downloadEnvelopeRequest: DownloadEnvelopeRequest) {

  def downloadEnvelopeRequest(envelopeId: String)
                             (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[String, Source[ByteString, _]]] =
    downloadEnvelopeRequest.executeGetRequest(envelopeId)
      .map { response =>
        if (response.status == 200) Right(response.bodyAsSource) else Left(response.body)
      }
      .recover { case ex =>
        Left(s"Unknown problem when trying to download an envelopeId $envelopeId: " + ex.getMessage)
      }
}

