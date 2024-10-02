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
import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion: String = "9.5.0"

  private val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % "10.0.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"      % bootstrapPlayVersion,
    "org.scalatest"          %% "scalatest"                   % "3.2.19",
    "org.scalatestplus"      %% "scalacheck-1-17"             % "3.2.18.0",
    "org.scalatestplus"      %% "mockito-5-10"                % "3.2.18.0",
    "org.wiremock"           %  "wiremock-standalone"         % "3.9.1",
    "org.jsoup"              %  "jsoup"                       % "1.18.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.18.0",
    "com.vladsch.flexmark"   % "flexmark-all"                 % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
