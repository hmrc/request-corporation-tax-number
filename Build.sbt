import play.core.PlayVersion
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.ForkedJvmPerTestSettings.oneForkedJvmPerTest
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}

val appName = "request-corporation-tax-number"

lazy val appDependencies: Seq[ModuleID] = compile ++ test
lazy val plugins : Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq.empty

val scope: String = "test,it"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-26" % "2.2.0",
  "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
  "uk.gov.hmrc" %% "json-encryption" % "4.8.0-play-26"
)

val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
  "org.scalatest" %% "scalatest" % "3.0.5" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "org.jsoup" % "jsoup" % "1.11.3" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
  "org.mockito" % "mockito-core" % "3.2.4" % scope,
  "org.scalacheck" %% "scalacheck" % "1.14.3" % scope,
  "com.github.tomakehurst" % "wiremock" % "2.26.0" % scope
)

// Fixes a transitive dependency clash between wiremock and scalatestplus-play Thanks Mac
val jettyFromWiremockVersion = "9.2.24.v20180105"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins : _*)
  .settings(playSettings : _*)
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
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))
  .settings(majorVersion := 1)

dependencyOverrides ++= Seq(
  "org.eclipse.jetty" % "jetty-client"                % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-continuation"          % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-http"                  % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-io"                    % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-security"              % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-server"                % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-servlet"               % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-servlets"              % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-util"                  % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-webapp"                % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty" % "jetty-xml"                   % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty.websocket" % "websocket-api"     % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty.websocket" % "websocket-client"  % jettyFromWiremockVersion % "test",
  "org.eclipse.jetty.websocket" % "websocket-common"  % jettyFromWiremockVersion % "test"
)