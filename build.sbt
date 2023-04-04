import play.core.PlayVersion
import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

scalaVersion := "2.13.10"

val appName = "request-corporation-tax-number"

lazy val appDependencies: Seq[ModuleID] = compile ++ test

val scope: String = "test,it"
val bootstrapPlayVersion: String = "7.14.0"
val scalaTestVersion: String = "3.2.9.0"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
  "uk.gov.hmrc" %% "domain"                    % "8.1.0-play-28",
  "uk.gov.hmrc" %% "json-encryption"           % "5.1.0-play-28"
)

val test: Seq[ModuleID] = Seq(
  "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapPlayVersion % scope,
  "com.typesafe.play"      %% "play-test"              % PlayVersion.current  % scope,
  "org.scalatestplus"      %% "mockito-3-4"            % scalaTestVersion     % scope,
  "org.scalatestplus"      %% "scalacheck-1-15"        % scalaTestVersion     % scope,
  "com.github.tomakehurst" % "wiremock-jre8"           % "2.35.0"             % scope,
  "org.jsoup"              % "jsoup"                   % "1.14.3"             % scope,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2"         % scope
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    PlayKeys.devSettings += "play.server.http.port" -> "9201"
  )
  .settings(
    coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*models.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;.*.template.scala;",
    coverageMinimumBranchTotal := 80,
    coverageMinimumStmtTotal := 80,
    coverageMinimumStmtPerPackage := 80,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    Test / parallelExecution := false,
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    scalacOptions -= "-Xmax-classfile-name",
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s",
      "-Wconf:cat=unused-imports&src=xml/.*:s"
    )
  )
  .settings(
    IntegrationTest / Keys.fork  := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false)
  .settings(majorVersion := 1)
