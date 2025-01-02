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
import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

scalaVersion := "2.13.14"

val appName = "request-corporation-tax-number"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scalaSettings)
  .settings(defaultSettings())
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    PlayKeys.playDefaultPort := 9201,
    majorVersion := 1,
    coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*models.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;.*.template.scala;",
    coverageMinimumBranchTotal := 80,
    coverageMinimumStmtTotal := 80,
    coverageMinimumStmtPerPackage := 80,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    Test / parallelExecution := false,
    scalacOptions -= "-Xmax-classfile-name",
    scalacOptions ++= Seq(
        "-Wconf:cat=unused-imports&src=routes/.*:s",
        "-Wconf:cat=unused-imports&src=html/.*:s",
        "-Wconf:cat=unused-imports&src=xml/.*:s"
    ),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "app" / "templates" / "fop",
    Test / unmanagedResourceDirectories += baseDirectory.value / "app" / "templates" / "fop"
  )

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
