import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

scalaVersion := "2.13.10"

val appName = "request-corporation-tax-number"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= AppDependencies.apply(),
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
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
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

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
