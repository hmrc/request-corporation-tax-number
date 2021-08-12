import play.core.PlayVersion
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.ForkedJvmPerTestSettings.oneForkedJvmPerTest
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "request-corporation-tax-number"

lazy val appDependencies: Seq[ModuleID] = compile ++ test

val scope: String = "test,it"
val silencerVersion: String = "1.7.1"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.10.0",
  "uk.gov.hmrc" %% "domain"                    % "6.2.0-play-28",
  "uk.gov.hmrc" %% "json-encryption"           % "4.10.0-play-28",
  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib"    % silencerVersion % Provided cross CrossVersion.full
)

val test: Seq[ModuleID] = Seq(
  "org.scalatestplus"      %% "mockito-3-4"        % "3.2.9.0"           % scope,
  "org.scalatestplus"      %% "scalacheck-1-15"    % "3.2.9.0"           % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"             % scope,
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope,
  "com.github.tomakehurst" % "wiremock-jre8"       % "2.30.0"            % scope,
  "org.pegdown"            % "pegdown"             % "1.6.0"             % scope,
  "org.jsoup"              % "jsoup"               % "1.14.1"            % scope,
  "com.vladsch.flexmark"   % "flexmark-all"        % "0.36.8"            % scope,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.0"    % scope
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.12.12")
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    PlayKeys.devSettings += "play.server.http.port" -> "9201"
  )
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*models.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;.*.template.scala;",
    ScoverageKeys.coverageMinimum := 94,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .settings(majorVersion := 1)

scalacOptions ++= Seq(
  "-P:silencer:pathFilters=templates;routes"
)
