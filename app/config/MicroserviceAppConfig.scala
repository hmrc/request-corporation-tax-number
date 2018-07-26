/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig


@Singleton
class MicroserviceAppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {
  override protected def mode = environment.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadBoolean(key: String) = runModeConfiguration.getBoolean(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadMilliseconds(key : String, default : Long) = runModeConfiguration.getMilliseconds(key).getOrElse(default)

  lazy val appName: String = runModeConfiguration.underlying.getString("appName")
  lazy val fileUploadUrl: String = baseUrl("file-upload")
  lazy val fileUploadFrontendUrl: String = baseUrl("file-upload-frontend")
  lazy val fileUploadCallbackUrl: String = loadConfig(s"microservice.services.file-upload.callbackUrl")

  lazy val pdfServiceUrl: String = baseUrl("pdf-generator-service")

  lazy val maxAttemptNumber: Int = 5
  lazy val firstRetryMilliseconds: Int = 20

  object CTUTR {

    lazy val businessArea : String = loadConfig(s"pdf.ctutr.metadata.businessArea")
    lazy val queue : String = loadConfig(s"pdf.ctutr.metadata.queue")
    lazy val formId : String = loadConfig(s"pdf.ctutr.metadata.formId")
    lazy val source : String = loadConfig(s"pdf.ctutr.metadata.source")
    lazy val target : String = loadConfig(s"pdf.ctutr.metadata.target")
    lazy val save : Boolean = loadBoolean(s"pdf.ctutr.save")

  }

}
