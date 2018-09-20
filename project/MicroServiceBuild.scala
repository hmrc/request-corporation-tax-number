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

import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "request-corporation-tax-number"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val mockitoAllVersion = "1.10.19"
  private val wireMockVersion = "2.15.0"
  private val scalacheckVersion = "1.13.4"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "3.7.0",
    "uk.gov.hmrc" %% "domain" % "5.2.0",
    "uk.gov.hmrc" %% "json-encryption" % "3.3.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.1.0" % scope,
    "org.scalatest" %% "scalatest" % "3.0.0" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.11.3" % "test,it",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope,
    "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope
  )
}
