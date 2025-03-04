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

package config

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class MicroserviceAppConfig @Inject()(servicesConfig: ServicesConfig) {

  private def loadConfig(key: String) = servicesConfig.getString(key)

  lazy val appName: String = servicesConfig.getString("appName")

  lazy val businessArea: String = loadConfig(s"pdf.ctutr.metadata.businessArea")
  lazy val queue: String = loadConfig(s"pdf.ctutr.metadata.queue")
  lazy val formId: String = loadConfig(s"pdf.ctutr.metadata.formId")
  lazy val source: String = loadConfig(s"pdf.ctutr.metadata.source")
  lazy val target: String = loadConfig(s"pdf.ctutr.metadata.target")
  lazy val save: String = servicesConfig.getBoolean(s"pdf.ctutr.metadata.save").toString
  lazy val authToken: String = servicesConfig.getString("internal-auth.token")
  lazy val dmsSubmissionBaseUrl: String = servicesConfig.baseUrl("dms-submission")
  lazy val dmsSubmissionCallbackUrl: String = loadConfig(s"microservice.services.dms-submission.callbackUrl")
}
