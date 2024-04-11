import sbt.Keys.scalacOptions
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

scalaVersion := "2.13.13"

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
      )
  )

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
