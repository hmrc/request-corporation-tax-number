ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.16"

lazy val microservice = Project("request-corporation-tax-number", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(CodeCoverageSettings())
  .settings(
    libraryDependencies ++= AppDependencies(),
    PlayKeys.playDefaultPort := 9201,
    scalacOptions -= "-Xmax-classfile-name",
    scalacOptions ++= Seq(
      "-Wconf:cat=unused-imports&src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s",
      "-Wconf:cat=unused-imports&src=xml/.*:s"
    ),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "app" / "templates" / "fop",
    Test / unmanagedResourceDirectories += baseDirectory.value / "app" / "templates" / "fop"
  )

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
